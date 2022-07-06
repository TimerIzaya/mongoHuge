package com.netease.cloud.lowcode.mongoHuge.pathEntity;


import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * partPath中的kv类型，比如 modules[name="xxx"]
 */
@Data
public class KvPath implements SegPath<String> {

    // modules[name="xxx"]中的modules
    String path;

    // modules[name="xxx"]中的name
    String key;

    // modules[name="xxx"]中的xxx
    String value;

    // KvPath和IdxPath属于一对多
    List<Integer> idxs = new ArrayList<>();

    public KvPath(String path, String key, String value) {
        this.path = path;
        this.key = key;
        this.value = value;
    }

    @Override
    public Type getType() {
        return Type.kv;
    }

    @Override
    public Map<String, String> get() {
        Map<String, String> map = new HashMap<>();
        map.put("key", key);
        map.put("value", value);
        return map;
    }

    @Override
    public String getPath() {
        return path;
    }
}
