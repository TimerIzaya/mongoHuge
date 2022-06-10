package com.netease.cloud.lowcode.naslstorage.repository;

import com.netease.cloud.lowcode.naslstorage.context.RepositoryOperationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public interface AppRepository {
    String APP = "app";
    String COLLECTION_NAME = APP;
    String APP_ID = "name";
    String OBJECT_ID = "_id";
    String NEED_SPLIT_DOC_VIEWS = "views";
    String NEED_SPLIT_DOC_CHILDREN = "children";
    String NEED_SPLIT_DOC_LOGICS = "logics";
    String REFERENCE_OBJECT_ID = "refId";
    /**
     * 应用node下第一层需要拆分出去的字段
     */
    List<String> NEED_SPLIT_FIRST_PROPERTY_IN_APP_DOC = Arrays.asList(NEED_SPLIT_DOC_VIEWS, NEED_SPLIT_DOC_LOGICS);
    Object get(RepositoryOperationContext context, String jsonPath, List<String> excludes);
    Map insert(String jsonPath, Map o);
    void update(String jsonPath, Map o);
}
