package com.netease.cloud.lowcode.naslstorage.backend;

import com.netease.cloud.lowcode.naslstorage.common.ApiBaseResult;
import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;

import java.util.List;

public interface BackendStore {
    List<Object> batchQuery(List<QueryDTO> queryDTOS);

    void batchAction(List<ActionDTO> actionDTOS) throws Exception;
}
