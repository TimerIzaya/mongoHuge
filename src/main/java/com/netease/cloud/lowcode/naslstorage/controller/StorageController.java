package com.netease.cloud.lowcode.naslstorage.controller;

import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;
import com.netease.cloud.lowcode.naslstorage.service.StorageService;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storage")
public class StorageController {
    @Resource
    private StorageService storageService;

    @PostMapping("/batch")
    public void batch(@RequestBody List<ActionDTO> actionDTOS) {
        if (CollectionUtils.isEmpty(actionDTOS)) {
            return;
        }

        storageService.batch(actionDTOS);
    }

    @PostMapping("/batchQuery")
    public List<Map> batchQuery(@RequestBody List<QueryDTO> queryDTOS) {
        if (CollectionUtils.isEmpty(queryDTOS)) {
            return new ArrayList<>();
        }

        return storageService.batchQuery(queryDTOS);
    }
}
