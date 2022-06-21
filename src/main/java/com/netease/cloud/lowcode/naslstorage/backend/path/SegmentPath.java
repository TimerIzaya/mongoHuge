package com.netease.cloud.lowcode.naslstorage.backend.path;

import java.util.Map;

public interface SegmentPath<T> {

    String getType();

    String getArrName();

    Map<String, T> get();
}
