package com.netease.cloud.lowcode.naslstorage.backend.mongo;

import com.netease.cloud.lowcode.naslstorage.common.Consts;
import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.NaslChangedInfoDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;
import com.netease.cloud.lowcode.naslstorage.backend.BackendStore;
import com.netease.cloud.lowcode.naslstorage.enums.ActionEnum;
import com.netease.cloud.lowcode.naslstorage.enums.ChangedNaslType;
import com.netease.cloud.lowcode.naslstorage.interceptor.AppIdContext;
import com.netease.cloud.lowcode.mongoHuge.MongoHuge;
import com.netease.cloud.lowcode.mongoHuge.pathEntity.QueryPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class MdbStore implements BackendStore {

    @Resource
    private MdbNaslChangedRecordRepository naslChangedRecordRepository;

    MongoHuge mongoHuge = new MongoHuge();

    @Override
    public List<Object> batchQuery(List<QueryDTO> queryDTOS) {
        if (CollectionUtils.isEmpty(queryDTOS)) {
            return new ArrayList<>();
        }
        return queryDTOS.stream().map(this::query).collect(Collectors.toList());
    }


    @Override
    public Object query(QueryDTO queryDTO) {
        List<String> excludes = queryDTO.getExcludes();
        String rawPath = queryDTO.getPath();
        // 去除前端输入的app.
        rawPath = rawPath.replace(Consts.APP + Consts.DOT, "").replace(Consts.APP, "");
        QueryPath queryPath = new QueryPath(rawPath);
        Object result = mongoHuge.jsonGet(AppIdContext.get(), queryPath);

        // 第一层exclude
        if (result instanceof Collection) {
            ((Collection<?>) result).forEach(v -> {
                if (v instanceof Map) {
                    for (String exclude : excludes) {
                        ((Map<?, ?>) v).remove(exclude);
                    }
                }
            });
        } else if (result instanceof Map) {
            for (String exclude : excludes) {
                ((Map<?, ?>) result).remove(exclude);
            }
        }

        return result;
    }


    /**
     * 1. Mongo推荐使用内嵌文档来解决一对多等问题，倾向于数据非关系化
     * 它认为单文档的原子性可以解决绝大部分所谓多文档事务问题，所以单机没有多文档事务
     * 2. 单机上让事务注解生效，只需要设置副本集群只有当前一个节点即可
     * 3. java-driver的mongo事务是提供retry的，但是spring集成的事务没有用，所以要自己配置retry
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = UncategorizedMongoDbException.class, exceptionExpression = "#{message.contains('WriteConflict')}", maxAttemptsExpression = "${mongodb.transaction.maxAttempts:128}", backoff = @Backoff(delayExpression = "${mongodb.transaction.backoff.delay:500}"))
    public void batchAction(List<ActionDTO> actionDTOS) throws Exception {
        for (ActionDTO actionDTO : actionDTOS) {
            long time = System.currentTimeMillis();
            solveOpt(actionDTO);
            System.out.println(actionDTO.getAction() + " time : " + (System.currentTimeMillis() - time));
        }
    }

    private void solveOpt(ActionDTO actionDTO) {
        String appId = AppIdContext.get(), action = actionDTO.getAction(), rawPath = actionDTO.getPath();
        Map<String, Object> obj = actionDTO.getObject();

        // 去除前端输入的app.
        rawPath = rawPath.replace(Consts.APP + Consts.DOT, "").replace(Consts.APP, "");
        QueryPath queryPath = new QueryPath(rawPath);

        if (ActionEnum.CREATE.getAction().equals(action)) {
            if (queryPath.isEmpty()) {
                // 初始化 前端要求增加时间戳
                obj.put(Consts.TIMESTAMP, System.currentTimeMillis());
                mongoHuge.jsonInit(appId, obj);
                return;
            }
            // create 前端要求增加时间戳
            obj.put(Consts.TIMESTAMP, System.currentTimeMillis());
            obj.put(Consts.UPDATE_BY_APP, appId);
            mongoHuge.jsonArrInsert(appId, queryPath, obj);
        } else if (ActionEnum.UPDATE.getAction().equals(action)) {
            // update 前端要求增加时间戳
            obj.put(Consts.TIMESTAMP, System.currentTimeMillis());
            obj.put(Consts.UPDATE_BY_APP, appId);
            mongoHuge.jsonSet(appId, queryPath, obj);
        } else if (ActionEnum.DELETE.getAction().equals(action)) {
            mongoHuge.jsonDel(appId, queryPath);
        }
    }


    @Override
    public void recordAppNaslChanged(String appId, ChangedNaslType naslType) {

    }

    @Override
    public NaslChangedInfoDTO queryAppNaslChangedInfo(String appId) {
        return null;
    }

}
