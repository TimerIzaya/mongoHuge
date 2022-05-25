package com.netease.cloud.lowcode.naslstorage.service.impl;

import com.netease.cloud.lowcode.naslstorage.service.PathConverter;
import javafx.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 实现自定义path 到mongodb 支持的jsonPath 转换
 * Pair 中的key 是分段的path路径，value 是过滤条件
 */
@Service
public class MdbPathConverter implements PathConverter<List<Pair<String, String>>> {
    @Override
    public List<Pair<String, String>> convert(String jsonPath) {
        List<Pair<String, String>> ret = new ArrayList<>();
        List<String> splitPaths = Arrays.asList(jsonPath.split("\\."));
        String prePath = "";
        for (String splitPath : splitPaths) {
            List<String> x = Arrays.asList(splitPath.split("\\["));
            String key = x.get(0);
            String value = null;
            if (StringUtils.hasLength(prePath)) {
                prePath = prePath + "." + key;
            } else {
                if (!key.equalsIgnoreCase("apps")) {
                    prePath = prePath + key;
                }
            }
            String path = prePath;
            if (x.size() > 1) {
                String y = x.get(1);
                y = y.replace("]", "").replace("\"", "");
                List<String> values = Arrays.asList(y.split("="));
                if (StringUtils.hasLength(prePath)) {
                    path = prePath + "." + values.get(0);
                } else {
                    path = values.get(0);
                }
                if (values.size()>1) {
                    value = values.get(1);
                }
            }
            Pair<String, String> p = new Pair<>(path, value);
            ret.add(p);
        }
        return ret;
    }

    @Override
    public String getPreviousPath(String path) {
        int index = path.lastIndexOf(".");
        if (index < 0){
            return null;
        }
        return path.substring(0, index);
    }

    @Override
    public String concatPath(String path1, String path2) {
        if (StringUtils.hasLength(path1) && StringUtils.hasLength(path2)) {
            return path1 + "." + path2;
        } else if (StringUtils.hasLength(path1) && !StringUtils.hasLength(path2)) {
            return path1;
        } else if (!StringUtils.hasLength(path1) && StringUtils.hasLength(path2)) {
            return path2;
        } else {
            return "";
        }
    }

    public static void main(String[] args) {
        MdbPathConverter converter = new MdbPathConverter();
        String test = "apps[id=\"ab878b5e\"]";
        System.out.println(converter.convert(test));

        System.out.println(converter.getPreviousPath("apps"));
    }
}
