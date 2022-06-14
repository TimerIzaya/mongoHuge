package com.netease.cloud.lowcode.naslstorage.repository;

import com.netease.cloud.lowcode.naslstorage.entity.path.PartPath;

import java.util.List;
import java.util.Map;

public interface AppBatchRepository {

    void initApp(Map<String, Object> object);

    void update(String outerPath, String innerPath, Map<String, Object> object);

    void create(String outerPath, String innerPath, Map<String, Object> object);

    void delete(String outerPath, String innerPath);

}
