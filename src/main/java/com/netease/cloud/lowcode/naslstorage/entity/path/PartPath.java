package com.netease.cloud.lowcode.naslstorage.entity.path;

import java.util.Map;

public interface PartPath<T> {

    String getType();

    String getArrName();

    Map<String, T> get();
}
