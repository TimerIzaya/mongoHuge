package com.netease.cloud.lowcode.naslstorage.common;

import java.util.Arrays;
import java.util.List;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/10 16:44
 */

public class Global {
    static public String APP = "app";
    static public String COLLECTION_NAME = APP;
    static public String APP_ID = "id";
    static public String OBJECT_ID = "_id";
    static public String NEED_SPLIT_DOC_VIEWS = "views";
    static public String NEED_SPLIT_DOC_CHILDREN = "children";
    static public String NEED_SPLIT_DOC_LOGICS = "logics";
    static public String REFERENCE_OBJECT_ID = "refId";

    static public String VIEWS = "views";
    static public String CHILDREN = "children";
    static public String LOGICS = "logics";
    static public String PATH_TYPE_IDX = "idx";
    static public String PATH_TYPE_FIELD = "field";
    static public String PATH_TYPE_RANGE = "range";
    static public String PATH_TYPE_KV = "kv";

    static public String ACTION_CREATE = "create";
    static public String ACTION_UPDATE = "update";
    static public String ACTION_DELETE = "delete";

    static public String TIMESTAMP = "timestamp";


    /**
     * 应用node下第一层需要拆分出去的字段
     */
    static public List<String> NEED_SPLIT_FIRST_PROPERTY_IN_APP_DOC = Arrays.asList(NEED_SPLIT_DOC_VIEWS, NEED_SPLIT_DOC_LOGICS);

}