package com.netease.cloud.lowcode.naslstorage.controller;

import com.netease.cloud.lowcode.naslstorage.backend.BackendStore;
import com.netease.cloud.lowcode.naslstorage.common.ApiBaseResult;
import com.netease.cloud.lowcode.naslstorage.common.ApiErrorCode;
import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.NaslChangedInfoDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;
import com.netease.cloud.lowcode.naslstorage.interceptor.AppIdContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/storage")
public class StorageController {

    @Resource
    private BackendStore backendStore;

    @PostMapping("/batchAction")
    public ApiBaseResult batch(@RequestBody List<ActionDTO> actionDTOS) {
        try {
            backendStore.batchAction(actionDTOS);
            return ApiBaseResult.successRet();
        } catch (Exception e) {
            log.error("批量操作文档失败", e);
            return ApiBaseResult.errorOf(ApiErrorCode.INTERNAL_SERVER_ERROR.getStatusCode(), ApiErrorCode.INTERNAL_SERVER_ERROR.getZnMessage());
        }
    }

    @PostMapping("/batchQuery")
    public ApiBaseResult batchQuery(@RequestBody List<QueryDTO> queryDTOS) {
        if (CollectionUtils.isEmpty(queryDTOS)) {
            return ApiBaseResult.successRet();
        }
        try {
            List<Object> result = backendStore.batchQuery(queryDTOS);
            return ApiBaseResult.successRet(result);
        } catch (Exception e) {
            log.error("查询失败，", e);
            return ApiBaseResult.errorOf(ApiErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/query")
    public ApiBaseResult query(@RequestParam("path") String path, @RequestParam(value = "excludes", required = false) List<String> excludes) {
        try {
            QueryDTO queryDTO = new QueryDTO();
            queryDTO.setPath(path);
            queryDTO.setExcludes(excludes);
            Object result = backendStore.query(queryDTO);
            return ApiBaseResult.successRet(result);
        } catch (Exception e) {
            log.error("查询失败，appId={}, path={}", AppIdContext.get(), path, e);
            return ApiBaseResult.errorOf(ApiErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

        @GetMapping("/appNaslChangedInfo")
    public ApiBaseResult queryAppNaslChangedInfo() {
        try {
            NaslChangedInfoDTO result = backendStore.queryAppNaslChangedInfo(AppIdContext.get());
            return ApiBaseResult.successRet(result);
        } catch (Exception e) {
            log.error("查询NASL 变更信息失败，appId={}, error=", AppIdContext.get(), e);
            return ApiBaseResult.errorOf(ApiErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}

