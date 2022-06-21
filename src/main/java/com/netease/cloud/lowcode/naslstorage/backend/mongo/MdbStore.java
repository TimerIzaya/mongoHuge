package com.netease.cloud.lowcode.naslstorage.backend.mongo;

import com.netease.cloud.lowcode.naslstorage.common.ApiBaseResult;
import com.netease.cloud.lowcode.naslstorage.common.ApiErrorCode;
import com.netease.cloud.lowcode.naslstorage.common.Consts;
import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;
import com.netease.cloud.lowcode.naslstorage.backend.PathConverter;
import com.netease.cloud.lowcode.naslstorage.backend.BackendStore;
import com.netease.cloud.lowcode.naslstorage.util.PathUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
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
    private MdbSplitQueryRepositoryImpl appQueryRepository;

    @Autowired
    private MdbUpdateRepository mdbUpdateRepository;

    @Resource(name = "mdbPathConverter")
    private PathConverter pathConverter;

    @Resource
    MongoTemplate mongoTemplate;


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
     * Mongo推荐使用内嵌文档来解决一对多等问题，倾向于数据非关系化
     * 它认为单文档的原子性可以解决绝大部分所谓多文档事务问题，所以单机没有多文档事务
     * 事务注解生效需要配置副本，单机上实现事务只需要设置副本集群只有当前一个节点即可
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiBaseResult batchAction(List<ActionDTO> actionDTOS) {
        try {
            actionDTOS.forEach(this::solve);
            return ApiBaseResult.successRet();
        } catch (Exception e) {
            e.printStackTrace();
            return ApiBaseResult.errorOf(ApiErrorCode.INTERNAL_SERVER_ERROR.getStatusCode(), ApiErrorCode.INTERNAL_SERVER_ERROR.getZnMessage());
        }
    }


    private void solve(ActionDTO actionDTO) {
        String action = actionDTO.getAction(), rawPath = actionDTO.getPath();
        log.info("QueryPath: " + rawPath);
        Map<String, Object> object = actionDTO.getObject();
        // path为app则初始化app或者删除app
        if (Consts.APP.equals(rawPath)) {
            if (Consts.ACTION_CREATE.equals(action)) {
                // 如果存在先删除再生成，否则不会覆盖
                mdbUpdateRepository.deleteApp(Consts.APP);
                mdbUpdateRepository.initApp(actionDTO.getObject());
                return;
            } else if (Consts.ACTION_DELETE.equals(action)) {
                mdbUpdateRepository.deleteApp(Consts.APP);
                return;
            }
        }

        String[] splits = PathUtil.splitJsonPath(rawPath);
        if (Consts.ACTION_CREATE.equals(action)) {
            mdbUpdateRepository.create(splits[0], splits[1], object);
        } else if (Consts.ACTION_UPDATE.equals(action)) {
            mdbUpdateRepository.update(splits[0], splits[1], object);
        } else if (Consts.ACTION_DELETE.equals(action)) {
            mdbUpdateRepository.delete(splits[0], splits[1]);
        }
    }
}
