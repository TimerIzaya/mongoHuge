package com.netease.cloud.lowcode.naslstorage.service;

import com.netease.cloud.lowcode.naslstorage.common.ApiBaseResult;
import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;

import java.util.List;

public interface StorageService {

    List<Object> batchQuery(List<QueryDTO> queryDTOS);

    ApiBaseResult batch(List<ActionDTO> actionDTOS);
}
