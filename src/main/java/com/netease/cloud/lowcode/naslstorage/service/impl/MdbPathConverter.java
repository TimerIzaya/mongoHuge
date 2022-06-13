package com.netease.cloud.lowcode.naslstorage.service.impl;

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
@Service
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


    public static void main(String[] args) {
        MdbPathConverter converter = new MdbPathConverter();
        String test = "apps[id=\"ab878b5e\"]";
        System.out.println(converter.convert(test));
    }
}
