package com.netease.cloud.lowcode.naslstorage.repository;

import com.netease.cloud.lowcode.naslstorage.common.ApiBaseResult;

import java.util.Map;

public interface MdbUpdateRepository {

    void initApp(Map<String, Object> object);

    void deleteApp(String appId);

    void update(String outerPath, String innerPath, Map<String, Object> object);

    void create(String outerPath, String innerPath, Map<String, Object> object);

    void delete(String outerPath, String innerPath);

}
