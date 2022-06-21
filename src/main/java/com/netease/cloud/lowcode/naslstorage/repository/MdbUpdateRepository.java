package com.netease.cloud.lowcode.naslstorage.repository;

import com.netease.cloud.lowcode.naslstorage.common.ApiBaseResult;

import java.util.Map;

public interface MdbUpdateRepository {

    ApiBaseResult initApp(Map<String, Object> object);

    ApiBaseResult deleteApp(String appId);

    ApiBaseResult update(String outerPath, String innerPath, Map<String, Object> object);

    ApiBaseResult create(String outerPath, String innerPath, Map<String, Object> object);

    ApiBaseResult delete(String outerPath, String innerPath);

}
