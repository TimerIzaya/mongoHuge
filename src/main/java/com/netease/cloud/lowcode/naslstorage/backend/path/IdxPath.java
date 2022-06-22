package com.netease.cloud.lowcode.naslstorage.backend.path;


import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * partPath中的idx类型，比如 modules[1]
 */
@Data
public class IdxPath implements SegmentPath<Integer> {

    String path;

    /**
     * modules[1]中的1
     */
    int idx;

    public IdxPath(String path, int idx) {
        this.path = path;
        this.idx = idx;
    }

    @Override
    public SegmentPathType getType() {
        return SegmentPathType.idx;
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
