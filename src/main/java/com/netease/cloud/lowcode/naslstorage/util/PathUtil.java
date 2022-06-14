package com.netease.cloud.lowcode.naslstorage.util;


import com.netease.cloud.lowcode.naslstorage.entity.LocationDocument;
import com.netease.cloud.lowcode.naslstorage.repository.impl.SplitMdbAppRepositoryImpl;
import com.netease.cloud.lowcode.naslstorage.service.JsonPathSchema;
import com.netease.cloud.lowcode.naslstorage.service.PathConverter;
import org.bson.types.ObjectId;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

public class PathUtil {

    /**
     * @description:
     * 增删改模块使用的路径拆分算法
     * 由于查询模块和增删改模块对于outer path定义不同，所以拆分逻辑要单独实现
     * @return: outer & inner path
     */
    static public String[] splitJsonPath(String rawPath) {
        if ("app".equals(rawPath)) {
            return new String[]{"",""};
        }
        String[] splits = rawPath.split("\\.");
        // 增删改模块去除首个app字符串，start从1开始
        int start = 1, end = start;
        if (splits[start].contains("views[")) {
            end = start + 1;
            while (end < splits.length && splits[end].contains("children[")) {
                end++;
            }
        } else if (splits[start].contains("logics[")) {
            end = 2;
        }
        StringBuilder outer = new StringBuilder(), inner = new StringBuilder();
        for (int i = start; i < end; i++) {
            outer.append(splits[i]).append(".");
        }
        if (outer.length() > 0) {
            outer.deleteCharAt(outer.length() - 1);
        }
        for (int i = end; i < splits.length; i++) {
            inner.append(splits[i]).append(".");
        }
        if (inner.length() > 0) {
            inner.deleteCharAt(inner.length() - 1);
        }
        String[] ret = new String[2];
        ret[0] = outer.toString();
        ret[1] = inner.toString();
        return ret;
    }


    public static void main(String[] args) {
        String path = "app.logics[1].elements[0].name";
        String[] strings = PathUtil.splitJsonPath(path);
        System.out.println(Arrays.toString(strings));
    }
}
