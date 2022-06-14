package com.netease.cloud.lowcode.naslstorage.entity.path;


import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * partPath中的idx类型，比如 modules[1]
 */
@Data
public class IdxPath implements PartPath<Integer> {

    String arrName;

    /**
     * modules[1]中的1
     */
    int idx;

    public IdxPath(String arrName, int idx) {
        this.arrName = arrName;
        this.idx = idx;
    }

    @Override
    public String getType() {
        return "idx";
    }

    @Override
    public Map<String, Integer> get() {
        Map<String, Integer> map = new HashMap<>();
        map.put("idx", idx);
        return map;
    }
}
