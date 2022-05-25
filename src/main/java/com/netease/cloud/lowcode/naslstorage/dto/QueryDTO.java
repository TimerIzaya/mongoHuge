package com.netease.cloud.lowcode.naslstorage.dto;

import lombok.Data;

import java.util.List;

@Data
public class QueryDTO {
    /**
     * 能明确得确定某一个的节点的路径，节点的类型可能是 基本类型，也可能是对象或者数组
     */
    private String path;
    /**
     * 排除节点的部分属性的详情，是path 的相对路径
     */
    private List<String> excludes;
}
