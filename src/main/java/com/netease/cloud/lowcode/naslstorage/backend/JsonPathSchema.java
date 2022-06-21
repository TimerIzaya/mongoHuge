package com.netease.cloud.lowcode.naslstorage.backend;

import lombok.Data;

/**
 * 记录我们自定义的JsonPath 解析后的信息
 */
@Data
public class JsonPathSchema {
    /**
     * 路径
     */
    private String path;
    /**
     * 过滤 key
     */
    private String key;
    /**
     * key 对应的过滤值
     */
    private String value;
}
