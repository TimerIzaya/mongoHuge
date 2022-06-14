package com.netease.cloud.lowcode.naslstorage.entity;

import org.bson.types.ObjectId;

import java.util.List;

/**
 * @description: JsonPath中最终需要操作的文档
 * @author: sunhaoran
 * @time: 2022/6/10 14:45
 */

public class LocationDocument {
    private List<ObjectId> objectIds;
    private String innerJsonPath;
    private String outJsonPath;

    public List<ObjectId> getObjectIds() {
        return objectIds;
    }

    public String getInnerJsonPath() {
        return innerJsonPath;
    }

    public void setInnerJsonPath(String innerJsonPath) {
        this.innerJsonPath = innerJsonPath;
    }

    public void setObjectIds(List<ObjectId> objectIds) {
        this.objectIds = objectIds;
    }

    public String getOutJsonPath() {
        return outJsonPath;
    }

    public void setOutJsonPath(String outJsonPath) {
        this.outJsonPath = outJsonPath;
    }
}