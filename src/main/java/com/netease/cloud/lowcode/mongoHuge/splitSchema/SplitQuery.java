package com.netease.cloud.lowcode.mongoHuge.splitSchema;

import com.netease.cloud.lowcode.mongoHuge.pathEntity.QueryPath;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: 单条拆分语句
 * @author: sunhaoran
 * @time: 2022/6/28 13:51
 */

@Data
public class SplitQuery {

    /**
     * @description: 定位到要拆分的子文档
     * 比如: "modules.views"  表明要拆分modules.views字段
     * 比如: "modules.views[0].logics[0]"
     * @return:
     */
    private QueryPath location;

    private String splitTarget;

    /**
     * @description: 递归拆分的字段
     */
    public String recurField;

    /**
     * @description: 设置拆分后文档之间关联的字段
     * 为指定的关联字段建立索引
     * 如果不指定关联字段，则遍历
     * 拆分用例: views
     * 路径用例: views[name = v1]
     * 如果指定name，则通过索引找到指定view，否则遍历引用链查找
     * @return:
     */
    private List<String> jointFields = new ArrayList<>();


}