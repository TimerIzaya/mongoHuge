package com.netease.cloud.lowcode.naslstorage.service.impl;

import com.netease.cloud.lowcode.naslstorage.common.Global;
import com.netease.cloud.lowcode.naslstorage.entity.path.*;
import com.netease.cloud.lowcode.naslstorage.service.JsonPathSchema;
import com.netease.cloud.lowcode.naslstorage.service.PathConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 实现自定义path 到mongodb 支持的jsonPath 转换
 */
@Service("mdbPathConverter")
public class MdbPathConverter implements PathConverter<List<JsonPathSchema>> {
    private static final String PATH_SPLITTER = "\\.";
    private static final Pattern SPLIT_PATH_PATTERN = Pattern.compile("(.+)\\[(\\d+|.+=.+)\\]");
    private static final String PARAM_SPLITTER = "=";

    @Override
    public List<JsonPathSchema> convert(String jsonPath) {
        List<JsonPathSchema> ret = new ArrayList<>();
        List<String> splitPaths = Arrays.asList(jsonPath.split(PATH_SPLITTER));
        for (String splitPath : splitPaths) {
            Matcher matcher = SPLIT_PATH_PATTERN.matcher(splitPath);
            JsonPathSchema pathSchema = new JsonPathSchema();
            if (!matcher.matches()) {
                pathSchema.setPath(splitPath);
            } else {
                pathSchema.setPath(matcher.group(1));
                List<String> param = Arrays.asList(matcher.group(2).split(PARAM_SPLITTER));
                if (param.size() > 1) {
                    pathSchema.setKey(param.get(0));
                    pathSchema.setValue(param.get(1));
                } else {
                    pathSchema.setValue(param.get(0));
                }
            }

            ret.add(pathSchema);
        }
        return ret;
    }

    @Override
    public String reverseConvert(List<JsonPathSchema> jsonPathSchemas) {
        if (CollectionUtils.isEmpty(jsonPathSchemas)) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < jsonPathSchemas.size(); i++){
            JsonPathSchema v = jsonPathSchemas.get(i);
            builder.append(v.getPath());
            if (StringUtils.hasLength(v.getKey()) && StringUtils.hasLength(v.getValue())) {
                builder.append("[");
                builder.append(v.getKey());
                builder.append("=");
                builder.append(v.getValue());
                builder.append("]");
            } else if (!StringUtils.hasLength(v.getKey()) && StringUtils.hasLength(v.getValue())) {
                builder.append("[");
                builder.append(v.getValue());
                builder.append("]");
            }
            if (i != jsonPathSchemas.size() -1) {
                builder.append(".");
            }
        }
        return builder.toString();
    }

    @Override
    public List<PartPath> pathForSetKey(String jsonPath) {
        List<PartPath> ret = new ArrayList<>();
        String[] splitPaths = jsonPath.split("\\.");
        for (String splitPath : splitPaths) {
            if (splitPath.equals(Global.APP)) {
                continue;
            }
            int i = splitPath.indexOf("["), j = splitPath.indexOf("]"), e = splitPath.indexOf("="), r = splitPath.indexOf(":");
            // FieldPath
            if (i == -1) {
                ret.add(new FieldPath(splitPath));
                break;
            }
            String arrName = splitPath.substring(0, i).trim();
            if (e != -1) {// KvPath
                String key = splitPath.substring(i + 1, e).trim();
                String value = splitPath.substring(e + 1, j).trim();
                ret.add(new KvPath(arrName, key, value));
            } else if (r != -1) { // RangePath
                int start = Integer.parseInt(splitPath.substring(i + 1, r).trim());
                int end = Integer.parseInt(splitPath.substring(r + 1, j).trim());
                ret.add(new RangePath(arrName, start, end));
            } else { // IdxPath
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
