package com.netease.cloud.lowcode.mongoHuge.pathSchema;

public enum PathType {
    /**
     * NaslPath针对单个Object，语法简单
     * Example: views[name=V1].elements[1:5].attributes
     */
    NaslPath,

    /**
     * JsonPath针对整个Json，语法复杂
     * Example: $[0].views[?(@.name=v1)].elements[1:5].attributes
     */
    JsonPath
}
