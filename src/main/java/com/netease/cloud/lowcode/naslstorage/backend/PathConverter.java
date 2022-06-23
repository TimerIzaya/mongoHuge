package com.netease.cloud.lowcode.naslstorage.backend;

import com.netease.cloud.lowcode.naslstorage.backend.path.SegmentPath;
import org.springframework.boot.SpringApplication;

import java.util.List;

/**
 * @param <T>
 * @author pingerchen
 */
public interface PathConverter<T> {
    /**
     * 自定义的jsonPath，格式如app.views[name=w333].children[name=12222].name，转换成对应的存储系统可识别的路径
     * 查询条件在每一段path 中，path 的数组类型node 可以通过[] 中查询条件过滤，非数组类型不能定义[]查询条件；app 的查询条件通过header 带入，也不能定义[]查询条件
     * 数组还可以通过[0] 下标来过滤
     *
     * @param jsonPath
     * @return
     */
    List<SegmentPath> convert(String jsonPath);

    /**
     * 将mdb 的path 反向转为自定义jsonPath
     *
     * @param t
     * @return
     */
    String reverseConvert(T t);

}
