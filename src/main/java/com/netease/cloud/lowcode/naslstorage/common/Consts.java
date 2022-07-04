package com.netease.cloud.lowcode.naslstorage.common;

import java.util.Arrays;
import java.util.List;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/10 16:44
 */

public class Consts {
    static public final String HEADER_APPID = "appId";
    static public final String APP = "app";
    static public final String COLLECTION_NAME = APP;
    static public final String APP_ID = "id";
    static public final String OBJECT_ID = "_id";
    static public final String NEED_SPLIT_DOC_VIEWS = "views";
    static public final String NEED_SPLIT_DOC_CHILDREN = "children";
    static public final String NEED_SPLIT_DOC_LOGICS = "logics";
    static public final String REFERENCE_OBJECT_ID = "refId";

    static public final String VIEWS = "views";
    static public final String CHILDREN = "children";
    static public final String LOGICS = "logics";
    static public final String NAME = "name";
    static public final String CONCEPT = "concept";
    static public final String CONCEPT_VIEW = "View";
    static public final String CONCEPT_LOGIC = "Logic";

    /**
     * TIMESTAMP 和UPDATE_BY_APP 是多人协作需要的
     */
    static public final String TIMESTAMP = "changedTime";
    static public final String UPDATE_BY_APP = "branchName";

     static public final String PATH_SPLITTER = "\\.";
     static public final String PARAM_START_TAG = "[";
     static public final String PARAM_END_TAG = "]";
     static public final String PARAM_SPLITTER = "=";
     static public final String ARR_SLICE_SPLITTER = ":";
     static public final String DOT = ".";

}