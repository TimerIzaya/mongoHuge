package com.netease.cloud.lowcode.mongoHuge.pathEntity;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class FieldPath implements SegPath<String> {

    String value;

    @Override
    public Type getType() {
        return Type.field;
    }

    @Override
    public String getPath() {
        return value;
    }

    @Override
    public Map<String, String> get() {
        Map<String, String> map = new HashMap<>();
        map.put("field", value);
        return map;
    }

    public FieldPath(String value) {
        this.value = value;
    }
}
