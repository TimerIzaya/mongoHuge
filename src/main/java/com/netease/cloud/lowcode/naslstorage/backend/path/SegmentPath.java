package com.netease.cloud.lowcode.naslstorage.backend.path;

import java.util.Map;

public interface SegmentPath<T> {

    SegmentPathType getType();

    String getPath();

    Map<String, T> get();

    enum SegmentPathType {
        idx("idx", "数组索引"),
        kv("kv", "健值对"),
        field("field", "属性值"),
        range("range", "数组切片");
        String type;
        String note;
        SegmentPathType(String type, String note) {
            this.type = type;
            this.note = note;
        }
    }
}
