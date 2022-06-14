package com.netease.cloud.lowcode.naslstorage.entity.path;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class RangePath implements PartPath<Integer> {
    /**
     * modules[1:-2]中的modules
     */
    String arrName;

    /**
     * modules[1:-2]中的1
     */
    int start;

    /**
     * modules[1:-2]中的-2
     */
    int end;

    @Override
    public String getType() {
        return "range";
    }

    @Override
    public Map<String, Integer> get() {
        Map<String, Integer> map = new HashMap<>();
        map.put("start", start);
        map.put("end", end);
        return map;
    }

    public RangePath(String arrName, int start, int end) {
        this.arrName = arrName;
        this.start = start;
        this.end = end;
    }
}
