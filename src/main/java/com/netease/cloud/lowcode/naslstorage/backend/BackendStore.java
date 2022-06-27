package com.netease.cloud.lowcode.naslstorage.backend;

import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;

import java.util.List;

public interface BackendStore {
    List<Object> batchQuery(List<QueryDTO> queryDTOS);

    Object query(QueryDTO queryDTO);

    void batchAction(List<ActionDTO> actionDTOS) throws Exception;
}
