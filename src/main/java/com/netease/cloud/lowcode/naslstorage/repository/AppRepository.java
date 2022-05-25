package com.netease.cloud.lowcode.naslstorage.repository;

import java.util.List;
import java.util.Map;

public interface AppRepository {
    Map get(String jsonPath, List<String> excludes);
    Map insert(String jsonPath, Map o);
    void update(String jsonPath, Map o);
}
