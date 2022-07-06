package com.netease.cloud.lowcode.naslstorage.backend.mongo;

import com.netease.cloud.lowcode.naslstorage.common.Consts;
import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.NaslChangedInfoDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;
import com.netease.cloud.lowcode.naslstorage.backend.BackendStore;
import com.netease.cloud.lowcode.naslstorage.enums.ActionEnum;
import com.netease.cloud.lowcode.naslstorage.backend.path.PathUtil;
import com.netease.cloud.lowcode.naslstorage.enums.ChangedNaslType;
import com.netease.cloud.lowcode.naslstorage.interceptor.AppIdContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class MdbStore implements BackendStore {
    @Resource
    private MdbNaslChangedRecordRepository naslChangedRecordRepository;

    @Resource(name = "splitMdbAppRepositoryImpl")
    private MdbSplitQueryRepository appQueryRepository;

    @Autowired
    private MdbAppUpdateRepository mdbAppUpdateRepository;

    @Override
    public List<Object> batchQuery(List<QueryDTO> queryDTOS) {
        if (CollectionUtils.isEmpty(queryDTOS)) {
            return new ArrayList<>();
        }
        return queryDTOS.stream().map(this::query).collect(Collectors.toList());
    }


    @Override
    public Object query(QueryDTO queryDTO) {
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
    @Retryable(value = UncategorizedMongoDbException.class, exceptionExpression = "#{message.contains('WriteConflict')}", maxAttemptsExpression = "${mongodb.transaction.maxAttempts:10}", backoff = @Backoff(delayExpression = "${mongodb.transaction.backoff.delay:100}"))
    public void batchAction(List<ActionDTO> actionDTOS) throws Exception {
        for (ActionDTO actionDTO : actionDTOS) {
            solveOpt(actionDTO);
        }
    }

    @Override
    public void recordAppNaslChanged(String appId, ChangedNaslType naslType) {
        naslChangedRecordRepository.recordAppNaslChanged(appId, naslType);
    }

    @Override
    public NaslChangedInfoDTO queryAppNaslChangedInfo(String appId) {
        return naslChangedRecordRepository.queryAppNaslChangedInfo(appId);
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

        String[] splits = PathUtil.splitPathForUpdate(rawPath);
        if (ActionEnum.CREATE.getAction().equals(action)) {
            mdbAppUpdateRepository.create(splits[0], splits[1], object);
        } else if (ActionEnum.UPDATE.getAction().equals(action)) {
            mdbAppUpdateRepository.update(splits[0], splits[1], object);
        } else if (ActionEnum.DELETE.getAction().equals(action)) {
            mdbAppUpdateRepository.delete(splits[0], splits[1]);
        }
    }
}
