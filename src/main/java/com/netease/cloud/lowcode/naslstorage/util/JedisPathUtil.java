package com.netease.cloud.lowcode.naslstorage.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/16 14:31
 */

public class JedisPathUtil {
    private static final String PATH_SPLITTER = "\\.";
    private static final Pattern SPLIT_PATH_PATTERN = Pattern.compile("(.+)\\[(\\d+|.+=.+)\\]");
    private static final String PARAM_SPLITTER = "=";

    public static String convert(String jsonPath) {
        StringBuilder ret = new StringBuilder();

        List<String> splitPaths = Arrays.asList(jsonPath.split(PATH_SPLITTER));
        for (String splitPath : splitPaths) {
            if ("app".equals(splitPath)) {
                continue;
            }
            Matcher matcher = SPLIT_PATH_PATTERN.matcher(splitPath);
            if (!matcher.matches()) {
                ret.append(splitPath);
            } else {
                String arrName = matcher.group(1);
                String param = matcher.group(2);
                if (!param.contains(PARAM_SPLITTER)) {
                    ret.append(splitPath);
                } else {
                    ret.append(arrName);
                    String[] split = param.split(PARAM_SPLITTER);
                    String k = split[0], v = split[1];
                    String xpath = "[?(@." + k + "=='" + v + "')]";
                    ret.append(xpath);
                }
            }
            ret.append(".");
        }
        if (ret.length() > 0) {
            ret.deleteCharAt(ret.length() - 1);
        }
        return ret.toString();
    }
}