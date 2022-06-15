package com.netease.cloud.lowcode.naslstorage.repository;

import com.netease.cloud.lowcode.naslstorage.context.RepositoryOperationContext;

import java.util.List;

public interface AppRepository {

    Object get(RepositoryOperationContext context, String jsonPath, List<String> excludes);
}
