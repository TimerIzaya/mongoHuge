package com.netease.cloud.lowcode.mongoHuge.pathSchema;


import com.netease.cloud.lowcode.mongoHuge.common.Consts;
import com.netease.cloud.lowcode.mongoHuge.pathEntity.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: jsonPath和naslPath初始化
 * @author: sunhaoran
 * @time: 2022/6/29 16:31
 */

public class PathConvertor {

    static public List<SegPath> convertRawNaslPathToNaslPath(String rawPath) {
        if (rawPath.isEmpty()) {
            return new ArrayList<>();
        }
        List<SegPath> ret = new ArrayList<>();
        String[] splitPaths = rawPath.split(Consts.PATH_SPLITTER);
        for (String splitPath : splitPaths) {
            int i = splitPath.indexOf(Consts.PARAM_START_TAG), j = splitPath.indexOf(Consts.PARAM_END_TAG);
            int e = splitPath.indexOf(Consts.PARAM_SPLITTER), r = splitPath.indexOf(Consts.ARR_SLICE_SPLITTER);
            if (i == -1) {
                // FieldPath
                ret.add(new FieldPath(splitPath));
                continue;
            }
            String arrName = splitPath.substring(0, i).trim();
            if (e != -1) {
                // KvPath
                String key = splitPath.substring(i + 1, e).trim();
                String value = splitPath.substring(e + 1, j).trim();
                ret.add(new KvPath(arrName, key, value));
            } else if (r != -1) {
                // RangePath
                int start = Integer.parseInt(splitPath.substring(i + 1, r).trim());
                int end = Integer.parseInt(splitPath.substring(r + 1, j).trim());
                ret.add(new RangePath(arrName, start, end));
            } else {
                // IdxPath
                int idx = Integer.parseInt(splitPath.substring(i + 1, j).trim());
                ret.add(new IdxPath(arrName, idx));
            }
        }
        return ret;
    }

    // todo
    static public List<SegPath> convertRawJsonPathToNaslPath(String rawPath) {
        List<SegPath> ret = new ArrayList<>();

        return ret;
    }
}