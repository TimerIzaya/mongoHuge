package com.netease.cloud.lowcode.naslstorage.repository;

import com.netease.cloud.lowcode.naslstorage.context.RepositoryOperationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public interface AppRepository {

    Object get(RepositoryOperationContext context, String jsonPath, List<String> excludes);
}
