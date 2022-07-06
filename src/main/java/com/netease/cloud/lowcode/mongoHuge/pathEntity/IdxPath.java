package com.netease.cloud.lowcode.mongoHuge.pathEntity;


import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * partPath中的idx类型，比如 modules[1]
 */
@Data
public class IdxPath implements SegPath<Integer> {

    String path;

    // modules[1]中的1
    int idx;

    public IdxPath(String path, int idx) {
        this.path = path;
        this.idx = idx;
    }

    @Override
    public Type getType() {
        return Type.idx;
    }

    @Override
    public Map<String, Integer> get() {
        Map<String, Integer> map = new HashMap<>();
        map.put("idx", idx);
        return map;
    }

    @Override
    public String getPath() {
        return path;
    }
}
