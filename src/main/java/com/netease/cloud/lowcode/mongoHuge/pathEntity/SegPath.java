package com.netease.cloud.lowcode.mongoHuge.pathEntity;

import java.util.Map;

public interface SegPath<T> {

    Type getType();

    String getPath();

    Map<String, T> get();

    enum Type {
        idx("idx", "数组索引"),
        kv("kv", "健值对"),
        field("field", "属性值"),
        range("range", "数组切片");
        String type;
        String note;

        Type(String type, String note) {
            this.type = type;
            this.note = note;
        }
    }
}
