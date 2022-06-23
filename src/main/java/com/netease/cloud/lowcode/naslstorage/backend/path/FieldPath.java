package com.netease.cloud.lowcode.naslstorage.backend.path;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class FieldPath implements SegmentPath<String> {

    String value;

    @Override
    public SegmentPathType getType() {
        return SegmentPathType.field;
    }

    @Override
    public String getPath() {
        return value;
    }

    @Override
    public Map<String, String> get() {
        Map<String, String> map = new HashMap<>();
        map.put("key", value);
        return null;
    }

    public FieldPath(String value) {
        this.value = value;
    }
}
