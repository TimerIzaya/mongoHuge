package com.netease.cloud.lowcode.naslstorage.interceptor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppIdContext {

    private static ThreadLocal<String> threadLocal = new InheritableThreadLocal();

    public static String get() {
        return threadLocal.get();
    }

    public static void set(String appId) {
        threadLocal.set(appId);
    }

    public static void clear() {
        threadLocal.remove();
    }

}
