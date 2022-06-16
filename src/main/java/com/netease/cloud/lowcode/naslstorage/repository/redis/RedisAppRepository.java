package com.netease.cloud.lowcode.naslstorage.repository.redis;

import com.netease.cloud.lowcode.naslstorage.context.RepositoryOperationContext;

import java.util.List;
import java.util.Map;

public interface RedisAppRepository {

    Object get(RepositoryOperationContext context, String jsonPath, List<String> excludes);

    void initApp(Map<String, Object> object);

    void update(String path, Map<String, Object> object);

    void create(String path, Map<String, Object> object);

    void delete(String path);
}
