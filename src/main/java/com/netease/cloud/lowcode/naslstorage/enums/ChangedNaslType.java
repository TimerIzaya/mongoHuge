package com.netease.cloud.lowcode.naslstorage.enums;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/7/6 10:28
 */

/**
 * @author pingerchen
 * 前后端哪一部分代码有更新的枚举类
 */
public enum ChangedNaslType {
    none("none", "前后端代码都未变更"),
    web("web", "前端代码有更新"),
    backend("backend", "后端代码有更新"),
    both("both", "前后端代码都有更新");

    private String code;
    private String note;

    ChangedNaslType(String code, String note) {
        this.code = code;
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public String getCode() {
        return code;
    }

    public static ChangedNaslType from(String code) {
        for (ChangedNaslType c : values()) {
            if (c.getCode().equalsIgnoreCase(code)) {
                return c;
            }
        }
        return none;
    }
}
