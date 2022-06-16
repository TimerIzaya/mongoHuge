package com.netease.cloud.lowcode.naslstorage.repository.mongo;

import com.netease.cloud.lowcode.naslstorage.entity.path.PartPath;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.Map;


public interface MdbRepositoryUtil {

    /**
     * @description: 递归单个view对象并存储
     * @return: 新view，存放refId、name、children
     */
    Map saveView(Map view);

    List<Map> saveViews(List<Map> views);

    /**
     * @description: 存储单个logic对象
     * @return: 新logic，存放refId、name
     */
    Map saveLogic(Map logic);

    List<Map> saveLogics(List<Map> logics);

    /**
     * @description: 递归删除单个view对象
     * @return:
     */
    void deleteView(Map view);

    void deleteViews(List<Map> views);

    /**
     * @description: 删除单个logic
     * @return:
     */
    void deleteLogic(Map logic);

    void deleteLogics(List<Map> logics);

    /**
     * @description: 插入文档
     * @return: 插入文档的ObjectId
     */
    ObjectId insertDocument(Map object);

    /**
     * @description: 根据ObjectId删除文档
     * @return:
     */
    void removeDocument(ObjectId id, String collectionName);

    /**
     * @description: 根据传入路径生成对应的setKey
     * @return:
     */
    List<String> getSetKeys(List<PartPath> paths, int size, Update update);

}
