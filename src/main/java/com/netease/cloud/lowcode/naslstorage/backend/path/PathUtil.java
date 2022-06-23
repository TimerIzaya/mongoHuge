package com.netease.cloud.lowcode.naslstorage.backend.path;

import com.netease.cloud.lowcode.naslstorage.common.Consts;

import java.util.Arrays;

public class PathUtil {


    /**
     * @description: 增删改模块使用的路径拆分算法
     * 由于查询模块和增删改模块对于outer path定义不同，所以拆分逻辑要单独实现
     * @return: outer & inner path
     */
    static public String[] splitPathForUpdate(String rawPath) {
        if (Consts.APP.equals(rawPath)) {
            return new String[]{"", ""};
        }
        String[] splits = rawPath.split(Consts.PATH_SPLITTER);
        // 增删改模块去除首个app字符串，start从1开始
        int start = 1, end = start;
        if (splits[start].contains(Consts.VIEWS + Consts.PARAM_START_TAG)) {
            end = start + 1;
            while (end < splits.length && splits[end].contains(Consts.CHILDREN + Consts.PARAM_START_TAG)) {
                end++;
            }
        } else if (splits[start].contains(Consts.LOGICS + Consts.PARAM_START_TAG)) {
            end = 2;
        }
        StringBuilder outer = new StringBuilder(), inner = new StringBuilder();
        for (int i = start; i < end; i++) {
            outer.append(splits[i]).append(Consts.DOT);
        }
        if (outer.length() > 0) {
            outer.deleteCharAt(outer.length() - 1);
        }
        for (int i = end; i < splits.length; i++) {
            inner.append(splits[i]).append(Consts.DOT);
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
    }
}
