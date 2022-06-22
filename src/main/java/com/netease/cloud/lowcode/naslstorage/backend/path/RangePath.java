package com.netease.cloud.lowcode.naslstorage.backend.path;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class RangePath implements SegmentPath<Integer> {
    /**
     * modules[1:-2]中的modules
     */
    String path;

    /**
     * modules[1:-2]中的1
     */
    int start;

    /**
     * modules[1:-2]中的-2
     */
    int end;

    @Override
    public SegmentPathType getType() {
        return SegmentPathType.range;
    }

    @Override
    public Map<String, Integer> get() {
        Map<String, Integer> map = new HashMap<>();
        map.put("start", start);
        map.put("end", end);
        return map;
    }

    @Override
    public String getPath() {
        return path;
    }

    public RangePath(String path, int start, int end) {
        this.path = path;
        this.start = start;
        this.end = end;
    }
}
