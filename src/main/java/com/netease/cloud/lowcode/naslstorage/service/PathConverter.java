package com.netease.cloud.lowcode.naslstorage.service;

/**
 * @author pingerchen
 * @param <T>
 */
public interface PathConverter<T> {
    /**
     * 自定义的jsonPath，转换成对应的存储系统可识别的路径
     * 查询条件在每一段path 中，path 的最后一段可能有或没有查询条件
     * @param jsonPath
     * @return
     */
    T convert(String jsonPath);

    /**
     * 获取上一级path
     * @param path
     * @return
     */
    String getPreviousPath(String path);

    /**
     * 将path1和path2 拼装起来
     * @param path1
     * @param path2
     * @return
     */
    String concatPath(String path1, String path2);
}
