package com.netease.cloud.lowcode.naslstorage.service;

import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;

import java.util.List;
import java.util.Map;

public interface StorageService {
    List<Map> batchQuery(List<QueryDTO> queryDTOS);
    void batch(List<ActionDTO> actionDTOS);
}
