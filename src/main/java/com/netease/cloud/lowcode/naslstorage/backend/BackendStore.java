package com.netease.cloud.lowcode.naslstorage.backend;

import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.NaslChangedInfoDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;
import com.netease.cloud.lowcode.naslstorage.enums.ChangedNaslType;

import java.util.List;

public interface BackendStore {
    List<Object> batchQuery(List<QueryDTO> queryDTOS);

    Object query(QueryDTO queryDTO);

    void batchAction(List<ActionDTO> actionDTOS) throws Exception;

    /**
     * 记录应用nasl 变动时间
     * @param appId
     * @param naslType
     */
    void recordAppNaslChanged(String appId, ChangedNaslType naslType);

    NaslChangedInfoDTO queryAppNaslChangedInfo(String appId);
}
