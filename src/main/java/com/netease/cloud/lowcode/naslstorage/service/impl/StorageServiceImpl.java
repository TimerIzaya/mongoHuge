package com.netease.cloud.lowcode.naslstorage.service.impl;

import com.netease.cloud.lowcode.naslstorage.common.Global;
import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;
import com.netease.cloud.lowcode.naslstorage.entity.path.PartPath;
import com.netease.cloud.lowcode.naslstorage.repository.AppBatchRepository;
import com.netease.cloud.lowcode.naslstorage.repository.AppRepository;
import com.netease.cloud.lowcode.naslstorage.repository.RepositoryUtil;
import com.netease.cloud.lowcode.naslstorage.repository.impl.SplitMdbAppRepositoryImpl;
import com.netease.cloud.lowcode.naslstorage.service.JsonPathSchema;
import com.netease.cloud.lowcode.naslstorage.service.PathConverter;
import com.netease.cloud.lowcode.naslstorage.service.StorageService;
import com.netease.cloud.lowcode.naslstorage.util.PathUtil;
import javafx.util.Pair;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StorageServiceImpl implements StorageService {

    @Resource(name = "splitMdbAppRepositoryImpl")
    private SplitMdbAppRepositoryImpl appRepository;

    @Autowired
    private AppBatchRepository appBatchRepository;

    @Resource
    MdbPathConverter mdbPathConverter;

    @Resource
    private RepositoryUtil repositoryUtil;

    @Resource(name = "mdbPathConverter")
    private PathConverter pathConverter;

    @Autowired
    MongoTemplate mongoTemplate;


    @Override
    public List<Object> batchQuery(List<QueryDTO> queryDTOS) {
        if (CollectionUtils.isEmpty(queryDTOS)) {
            return new ArrayList<>();
        }
        return queryDTOS.stream().map(this::get).collect(Collectors.toList());
    }


    private Object get(QueryDTO queryDTO) {
        return appRepository.get(null, queryDTO.getPath(), queryDTO.getExcludes());
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
        Map<String, Object> object = actionDTO.getObject();
        // path为app则初始化app或者删除app
        if (Global.APP.equals(rawPath)) {
            if ("create".equals(action)) {
                mongoTemplate.dropCollection("app"); // used for test
                appBatchRepository.initApp(actionDTO.getObject());
                return;
            } else if ("delete".equals(action)) {
                mongoTemplate.dropCollection("app"); // used for test
                return;
            }
        }

        String[] splits = PathUtil.splitJsonPath(rawPath);
        if ("create".equals(action)) {
            appBatchRepository.create(splits[0], splits[1], object);
        } else if ("update".equals(action)) {
            appBatchRepository.update(splits[0], splits[1], object);
        } else if ("delete".equals(action)) {
            appBatchRepository.delete(splits[0], splits[1]);
        }
    }


}
