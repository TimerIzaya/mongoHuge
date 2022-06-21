package com.netease.cloud.lowcode.naslstorage.backend.path;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class FieldPath implements SegmentPath<String> {

    String value;

    @Override
    public String getType() {
        return "field";
    }

    @Override
    public String getArrName() {
        return "";
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
