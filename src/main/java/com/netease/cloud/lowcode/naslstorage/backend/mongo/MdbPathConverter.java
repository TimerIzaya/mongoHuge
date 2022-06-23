package com.netease.cloud.lowcode.naslstorage.backend.mongo;

import com.netease.cloud.lowcode.naslstorage.backend.path.*;
import com.netease.cloud.lowcode.naslstorage.backend.PathConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 实现自定义path 到mongodb 支持的jsonPath 转换
 */
@Service("mdbPathConverter")
public class MdbPathConverter implements PathConverter<List<SegmentPath>> {
    private static final String PATH_SPLITTER = "\\.";
    private static final String PARAM_START_TAG = "[";
    private static final String PARAM_END_TAG = "]";
    private static final String PARAM_SPLITTER = "=";
    private static final String ARR_SLICE_SPLITTER = ":";


    @Override
    public String reverseConvert(List<SegmentPath> segmentPaths) {
        if (CollectionUtils.isEmpty(segmentPaths)) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segmentPaths.size(); i++){
            SegmentPath v = segmentPaths.get(i);
            builder.append(v.getPath());
            if (SegmentPath.SegmentPathType.kv == v.getType()) {
                KvPath tmp = (KvPath) v;
                builder.append(PARAM_START_TAG);
                builder.append(tmp.getKey());
                builder.append(PARAM_SPLITTER);
                builder.append(tmp.getValue());
                builder.append(PARAM_END_TAG);
            } else if (SegmentPath.SegmentPathType.idx == v.getType()) {
                IdxPath tmp = (IdxPath) v;
                builder.append(PARAM_START_TAG);
                builder.append(tmp.getIdx());
                builder.append(PARAM_END_TAG);
            }
            if (i != segmentPaths.size() -1) {
                builder.append(".");
            }
        }
        return builder.toString();
    }

    @Override
    public List<SegmentPath> convert(String jsonPath) {
        List<SegmentPath> ret = new ArrayList<>();
        String[] splitPaths = jsonPath.split(PATH_SPLITTER);
        for (String splitPath : splitPaths) {
            int i = splitPath.indexOf(PARAM_START_TAG), j = splitPath.indexOf(PARAM_END_TAG), e = splitPath.indexOf(PARAM_SPLITTER), r = splitPath.indexOf(ARR_SLICE_SPLITTER);
            // FieldPath
            if (i == -1) {
                ret.add(new FieldPath(splitPath));
                break;
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


    public static void main(String[] args) {
        MdbPathConverter converter = new MdbPathConverter();
        String test = "apps.views[name=123].elements[0].name";
        System.out.println(converter.convert(test));
    }
}
