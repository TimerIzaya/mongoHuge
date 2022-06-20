package com.netease.cloud.lowcode.naslstorage.service.impl;

import com.netease.cloud.lowcode.naslstorage.common.Global;
import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;
import com.netease.cloud.lowcode.naslstorage.enums.ActionEnum;
import com.netease.cloud.lowcode.naslstorage.repository.mongo.MdbUpdateRepository;
import com.netease.cloud.lowcode.naslstorage.repository.mongo.impl.MdbSplitQueryRepositoryImpl;
import com.netease.cloud.lowcode.naslstorage.repository.redis.RedisAppRepository;
import com.netease.cloud.lowcode.naslstorage.service.PathConverter;
import com.netease.cloud.lowcode.naslstorage.service.StorageService;
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
     * 需要支持多文档事务，mongodb版本大于等于4
     * 默认情况下，MongoDB将自动中止任何运行超过60秒的多文档事务
     *
     * @param actionDTOS
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batch(List<ActionDTO> actionDTOS) {
        if (CollectionUtils.isEmpty(actionDTOS)) {
            return;
        }
        actionDTOS.forEach(this::solve);
    }


    private void solve(ActionDTO actionDTO) {
        String action = actionDTO.getAction(), rawPath = actionDTO.getPath();
        log.info("QueryPath: " + rawPath);
        Map<String, Object> object = actionDTO.getObject();
        // path为app则初始化app或者删除app
        if (Global.APP.equals(rawPath)) {
            if (Global.ACTION_CREATE.equals(action)) {
                mongoTemplate.dropCollection(Global.APP); // used for test
                mdbUpdateRepository.initApp(actionDTO.getObject());
                return;
            } else if (Global.ACTION_DELETE.equals(action)) {
                mongoTemplate.dropCollection(Global.APP); // used for test
                return;
            }
        }

        String[] splits = PathUtil.splitJsonPath(rawPath);
        if (Global.ACTION_CREATE.equals(action)) {
            mdbUpdateRepository.create(splits[0], splits[1], object);
        } else if (Global.ACTION_UPDATE.equals(action)) {
            mdbUpdateRepository.update(splits[0], splits[1], object);
        } else if (Global.ACTION_DELETE.equals(action)) {
            mdbUpdateRepository.delete(splits[0], splits[1]);
        }
    }


}
