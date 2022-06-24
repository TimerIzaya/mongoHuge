package com.netease.cloud.lowcode.naslstorage.backend.mongo;

import com.netease.cloud.lowcode.naslstorage.common.ApiBaseResult;
import com.netease.cloud.lowcode.naslstorage.common.ApiErrorCode;
import com.netease.cloud.lowcode.naslstorage.common.Consts;
import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;
import com.netease.cloud.lowcode.naslstorage.backend.BackendStore;
import com.netease.cloud.lowcode.naslstorage.enums.ActionEnum;
import com.netease.cloud.lowcode.naslstorage.backend.path.PathUtil;
import com.netease.cloud.lowcode.naslstorage.interceptor.AppIdContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("mongoService")
public class MdbStore implements BackendStore {

    @Resource(name = "splitMdbAppRepositoryImpl")
    private MdbSplitQueryRepository appQueryRepository;

    @Autowired
    private MdbAppUpdateRepository mdbAppUpdateRepository;

    @Override
    public List<Object> batchQuery(List<QueryDTO> queryDTOS) {
        if (CollectionUtils.isEmpty(queryDTOS)) {
            return new ArrayList<>();
        }
        return queryDTOS.stream().map(this::get).collect(Collectors.toList());
    }


    private Object get(QueryDTO queryDTO) {
        return appQueryRepository.get(null, queryDTO.getPath(), queryDTO.getExcludes());
    }


    /**
     * 1. Mongo推荐使用内嵌文档来解决一对多等问题，倾向于数据非关系化
     * 它认为单文档的原子性可以解决绝大部分所谓多文档事务问题，所以单机没有多文档事务
     * 2. 单机上让事务注解生效，只需要设置副本集群只有当前一个节点即可
     * 3. java-driver的mongo事务是提供retry的，但是spring集成的事务没有用，所以要自己配置retry
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = UncategorizedMongoDbException.class, exceptionExpression = "#{message.contains('WriteConflict error')}", maxAttempts = 128, backoff = @Backoff(delay = 50))
    public void batchAction(List<ActionDTO> actionDTOS) throws Exception {
        for (ActionDTO actionDTO : actionDTOS) {
            solveOpt(actionDTO);
        }
    }


    private void solveOpt(ActionDTO actionDTO) throws Exception {
        String appId = AppIdContext.get(), action = actionDTO.getAction(), rawPath = actionDTO.getPath();
        Map<String, Object> object = actionDTO.getObject();
        log.info("QueryPath: " + rawPath);

        // path为app则可能为初始化app或者删除app
        if (Consts.APP.equals(rawPath)) {
            if (ActionEnum.DELETE.getAction().equals(action)) {
                mdbAppUpdateRepository.deleteApp(appId);
                return;
            }
            if (ActionEnum.CREATE.getAction().equals(action)) {
                mdbAppUpdateRepository.deleteApp(appId);
                mdbAppUpdateRepository.initApp(actionDTO.getObject());
                return;
            }
        }

        // update和create需要校验object是否合法
        if (!ActionEnum.DELETE.getAction().equals(action) && !isObjectLegal(object)) {
            throw new Exception("Object is illegal");
        }

        String[] splits = PathUtil.splitPathForUpdate(rawPath);
        if (ActionEnum.CREATE.getAction().equals(action)) {
            mdbAppUpdateRepository.create(splits[0], splits[1], object);
        } else if (ActionEnum.UPDATE.getAction().equals(action)) {
            mdbAppUpdateRepository.update(splits[0], splits[1], object);
        } else if (ActionEnum.DELETE.getAction().equals(action)) {
            mdbAppUpdateRepository.delete(splits[0], splits[1]);
        }
    }

    /**
     * @description: 检查传入的object是否合法
     * 1. 一般字段的v不能为null
     * 2. views和logics必须是数组
     * @return:
     */
    private boolean isObjectLegal(Map<String, Object> o) {
        for (String k : o.keySet()) {
            if (Consts.VIEWS.equals(k) || Consts.LOGICS.equals(k)) {
                if (!(o.get(k) instanceof ArrayList)) {
                    return false;
                }
            }
            if (o.get(k) == null) {
                return false;
            }
        }
        return true;
    }
}
