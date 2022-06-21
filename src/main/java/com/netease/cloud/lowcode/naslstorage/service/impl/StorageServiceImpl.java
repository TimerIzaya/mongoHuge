package com.netease.cloud.lowcode.naslstorage.service.impl;

import com.netease.cloud.lowcode.naslstorage.common.ApiBaseResult;
import com.netease.cloud.lowcode.naslstorage.common.ApiErrorCode;
import com.netease.cloud.lowcode.naslstorage.common.Global;
import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;
import com.netease.cloud.lowcode.naslstorage.enums.ActionEnum;
import com.netease.cloud.lowcode.naslstorage.repository.MdbUpdateRepository;
import com.netease.cloud.lowcode.naslstorage.repository.impl.MdbSplitQueryRepositoryImpl;
import com.netease.cloud.lowcode.naslstorage.service.PathConverter;
import com.netease.cloud.lowcode.naslstorage.service.StorageService;
import com.netease.cloud.lowcode.naslstorage.util.PathUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.SessionSynchronization;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StorageServiceImpl implements StorageService {

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
    public List<ApiBaseResult> batch(List<ActionDTO> actionDTOS) {
        List<ApiBaseResult> ret = new ArrayList<>();
        actionDTOS.forEach(a -> ret.add(solve(a)));
        return ret;
    }


    private ApiBaseResult solve(ActionDTO actionDTO) {

        String action = actionDTO.getAction(), rawPath = actionDTO.getPath();
        log.info("QueryPath: " + rawPath);
        Map<String, Object> object = actionDTO.getObject();
        // path为app则初始化app或者删除app
        if (Global.APP.equals(rawPath)) {
            if (Global.ACTION_CREATE.equals(action)) {
                // 如果存在先删除再生成，否则不会覆盖
                mdbUpdateRepository.deleteApp(Global.APP);
                return mdbUpdateRepository.initApp(actionDTO.getObject());
            } else if (Global.ACTION_DELETE.equals(action)) {
                return mdbUpdateRepository.deleteApp(Global.APP);
            }
        }

        String[] splits = PathUtil.splitJsonPath(rawPath);
        if (Global.ACTION_CREATE.equals(action)) {
            return mdbUpdateRepository.create(splits[0], splits[1], object);
        } else if (Global.ACTION_UPDATE.equals(action)) {
            return mdbUpdateRepository.update(splits[0], splits[1], object);
        } else if (Global.ACTION_DELETE.equals(action)) {
            return mdbUpdateRepository.delete(splits[0], splits[1]);
        }

        return ApiBaseResult.errorOf(ApiErrorCode.INTERNAL_SERVER_ERROR.getStatusCode(), ApiErrorCode.INTERNAL_SERVER_ERROR.getZnMessage());
    }
}
