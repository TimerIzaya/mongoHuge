package com.netease.cloud.lowcode.naslstorage.util;

/**
 * 前端制定的jsonPath 格式：
 * apps[id="ab878b5e"].logics[1]
 * apps[id="ab878b5e"].views[name=w333].children[name=c333].children[name=c444]
 */
public class PathUtil {
    /**
     * 从jsonPath 看本次操作的对象concept 是否为app
     * @param jsonPath
     * @return
     */
    public static boolean isAppOps(String jsonPath) {
        if (jsonPath.equalsIgnoreCase("apps")) {
            return true;
        }
        return false;
    }

    public static boolean isViewOps(String jsonPath) {
        if (jsonPath.endsWith("views") ||
                jsonPath.endsWith("children")) {
            return true;
        }
        return false;
    }

    public static String getPathInApp(String jsonPath) {
        return "";
    }

    public static String getPathInView(String jsonPath) {
        return "";
    }
}
