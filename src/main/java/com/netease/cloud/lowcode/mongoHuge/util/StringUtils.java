package com.netease.cloud.lowcode.mongoHuge.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/28 13:52
 */

public class StringUtils {

    static public List<String> splitByDot(String s) {
        return Arrays.stream(s.split("\\.")).collect(Collectors.toList());
    }

}