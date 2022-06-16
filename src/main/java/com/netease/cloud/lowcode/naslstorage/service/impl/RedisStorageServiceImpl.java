package com.netease.cloud.lowcode.naslstorage.service.impl;

import com.netease.cloud.lowcode.naslstorage.common.Global;
import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;
import com.netease.cloud.lowcode.naslstorage.repository.redis.RedisAppRepository;
import com.netease.cloud.lowcode.naslstorage.service.StorageService;
import com.netease.cloud.lowcode.naslstorage.util.PathUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/16 14:43
 */


@Service("redisService")
public class RedisStorageServiceImpl implements StorageService {


    @Resource
    RedisAppRepository appRepository;

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

    @Override
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
                appRepository.initApp(actionDTO.getObject());
                return;
            }
        }

        String[] splits = PathUtil.splitJsonPath(rawPath);
        if ("create".equals(action)) {
            appRepository.create(rawPath, object);
        } else if ("update".equals(action)) {
            appRepository.update(rawPath, object);
        } else if ("delete".equals(action)) {
            appRepository.delete(rawPath);
        }
    }
}