package com.netease.cloud.lowcode.naslstorage.backend.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.client.result.UpdateResult;
import com.netease.cloud.lowcode.naslstorage.backend.PathConverter;
import com.netease.cloud.lowcode.naslstorage.backend.path.IdxPath;
import com.netease.cloud.lowcode.naslstorage.backend.path.KvPath;
import com.netease.cloud.lowcode.naslstorage.backend.path.SegmentPath;
import com.netease.cloud.lowcode.naslstorage.common.Consts;
import com.netease.cloud.lowcode.naslstorage.interceptor.AppIdContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.*;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/10 14:53
 */

@Slf4j
@Repository
public class MdbAppUpdateRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private MdbReferenceDocumentUpdateRepository repoUtil;

    @Resource(name = "mdbPathConverter")
    private PathConverter pathConverter;

    @Resource(name = "splitMdbAppRepositoryImpl")
    private MdbSplitQueryRepository splitUtil;

    public void initApp(Map<String, Object> object) {
        List<Map> saveViews = repoUtil.saveViews((List<Map>) object.get(Consts.VIEWS));
        List<Map> saveLogics = repoUtil.saveLogics((List<Map>) object.get(Consts.LOGICS));
        object.put(Consts.VIEWS, saveViews);
        object.put(Consts.LOGICS, saveLogics);
        // 前端要求增加时间戳
        object.put(Consts.TIMESTAMP, System.currentTimeMillis());
        repoUtil.insertDocument(object);
    }


    /**
     * @description: 在目标数组指定位置添加对象
     * @return:
     */
    public void create(String outerPath, String innerPath, Map<String, Object> object) {
        String appId = AppIdContext.get();
        // 前端要求增加时间戳
        object.put(Consts.TIMESTAMP, System.currentTimeMillis());
        if (outerPath.isEmpty()) {
            createWhenOuterEmpty(appId, innerPath, object);
        } else if (!innerPath.isEmpty()) {
            createWhenInnerExist(outerPath, innerPath, object);
        } else {
            createWhenInnerEmpty(appId, outerPath, object);
        }
    }


    public void deleteApp(String appId) {
        mongoTemplate.dropCollection(Consts.APP);
    }


    /**
     * 更新对象的字段
     */
    public void update(String outerPath, String innerPath, Map<String, Object> object) {
        String appId = AppIdContext.get();
        // 前端要求增加时间戳
        object.put(Consts.TIMESTAMP, System.currentTimeMillis());
        if (outerPath.isEmpty()) {
            updateWhenOuterEmpty(appId, innerPath, object);
        } else if (!innerPath.isEmpty()) {
            updateWhenInnerExist(outerPath, innerPath, object);
        } else {
            updateWhenInnerEmpty(appId, outerPath, object);
        }
    }

    /**
     * 删除对象或字段
     */
    public void delete(String outerPath, String innerPath) {
        String appId = AppIdContext.get();
        if (outerPath.isEmpty()) {
            deleteWhenOuterEmpty(appId, outerPath);
        } else if (!innerPath.isEmpty()) {
            deleteWhenInnerExist(appId, outerPath, innerPath);
        } else {
            deleteWhenInnerEmpty(appId, outerPath);
        }
    }


    /**
     * 不存在外部路径，不需要修改app结构
     * 直接对app文档进行create
     * 路径用例："path": "app.processes[2].properties[6]",
     */
    private void createWhenOuterEmpty(String appId, String innerPath, Map<String, Object> object) {
        Query query = new Query();
        query.addCriteria(Criteria.where(Consts.APP_ID).is(appId));
        List<SegmentPath> paths = pathConverter.convert(innerPath);
        createAtTargetIndex(query, paths, object);
    }


    /**
     * 存在外部路径，存在内部路径，不需要修改app结构
     * 对所有目标文档进行create
     * 路径用例："path": "app.views[2].children[3].children[4].elements[5].properties[6]"
     */
    private void createWhenInnerExist(String outerPath, String innerPath, Map<String, Object> object) {
        List<ObjectId> targetDocIds = getFinalDocs(outerPath);
        Query query = new Query();
        for (ObjectId targetDocId : targetDocIds) {
            query.addCriteria(Criteria.where(Consts.OBJECT_ID).is(targetDocId));
        }
        List<SegmentPath> paths = pathConverter.convert(innerPath);
        createAtTargetIndex(query, paths, object);
    }


    /**
     * 存在外部路径，不存在内部路径，需要修改app结构
     * 路径用例: "path": "app.views[2].children[3].children[4]"
     */
    private void createWhenInnerEmpty(String appId, String outerPath, Map<String, Object> object) {
        // 先保存生成的view子文档
        Map map = repoUtil.saveView(object);
        // 更新app结构
        Query query = new Query(Criteria.where(Consts.APP_ID).is(appId));
        List<SegmentPath> paths = pathConverter.convert(outerPath);
        createAtTargetIndex(query, paths, map);
    }


    /**
     * @description: 确认最终文档，在数组指定位置添加对象
     * @return:
     */
    private void createAtTargetIndex(Query query, List<SegmentPath> paths, Map<String, Object> object) {
        Update update = new Update();
        // 最后一个path是数组和索引，不用解析到setKey
        List<String> setKeys = repoUtil.getSetKeys(paths, paths.size() - 1, update);
        for (String setKey : setKeys) {
            String finalSetKey = setKey.isEmpty() ? "" : setKey + ".";
            IdxPath lastIdxPath = (IdxPath) paths.get(paths.size() - 1);
            finalSetKey += lastIdxPath.getPath(); // 根据lastPath确定要create的数组
            int idx = lastIdxPath.getIdx(); // // 根据lastPath确定要create的数组位置
            update.push(finalSetKey).atPosition(idx).value(object);
        }
        UpdateResult app = mongoTemplate.updateFirst(query, update, Consts.COLLECTION_NAME);
        printUpdateResult(app);
    }


    /**
     * 不存在外部路径
     * 特殊情况：更新app.views和app.logics，需要同步更新app结构
     * 路径用例："path": "app",
     */
    private void updateWhenOuterEmpty(String appId, String innerPath, Map<String, Object> object) {
        Query query = new Query(Criteria.where(Consts.APP_ID).is(appId));
        List<SegmentPath> paths = pathConverter.convert(innerPath);
        for (String key : object.keySet()) {
            if (Consts.VIEWS.equals(key)) {
                // 删除旧views数组
                repoUtil.deleteViews(getViews(appId));
                // 保存新views数组
                List<Map> refIds = repoUtil.saveViews((List<Map>) object.get(key));
                object.put(key, refIds);
            } else if (Consts.LOGICS.equals(key)) {
                // 删除旧logics数组
                repoUtil.deleteLogics(getLogics(appId));
                // 保存新logics数组
                List<Map> refIds = repoUtil.saveLogics((List<Map>) object.get(key));
                object.put(key, refIds);
            }
        }
        updateWhenSetKeyDone(query, paths, object);
    }


    /**
     * 存在外部路径，存在内部路径
     * 对所有目标文档进行update，无特殊情况
     * 路径用例："path": "app.views[2].children[3].elements[5].properties[6]"
     */
    private void updateWhenInnerExist(String outerPath, String innerPath, Map<String, Object> object) {
        Query query = new Query();
        List<ObjectId> targetDocIds = getFinalDocs(outerPath);
        for (ObjectId targetDocId : targetDocIds) {
            query.addCriteria(Criteria.where(Consts.OBJECT_ID).is(targetDocId));
        }
        List<SegmentPath> paths = pathConverter.convert(innerPath);
        updateWhenSetKeyDone(query, paths, object);
    }


    /**
     * 存在外部路径，不存在内部路径
     * 特殊情况：更新name、更新children
     * 注意！！！更新name需要特别处理，放在最后更新，防止paths的lastPath是跟据name定位的
     */
    private void updateWhenInnerEmpty(String appId, String outerPath, Map<String, Object> object) {
        // 保留非特殊字段，批量修改
        Map<String, Object> commonField = new HashMap<>();
        boolean nameExist = false;
        for (String key : object.keySet()) {
            if ("name".equals(key)) {
                // 修改name字段 最后执行
                nameExist = true;
            } else if (Consts.CHILDREN.equals(key)) {
                // 先删除children子文档
                String rawPath = Consts.APP + "." + outerPath;
                Map<String, Object> view = (Map<String, Object>) splitUtil.get(null, rawPath, new ArrayList<>());
                repoUtil.deleteViews((List<Map>) view.get(Consts.CHILDREN));

                // 再根据object生成children
                List<Map> children = (List<Map>) object.get(key);
                List<Map> refIds = repoUtil.saveViews(children);

                // 同步更新当前view子文档的结构
                List<ObjectId> finalDocIds = getFinalDocs(outerPath);
                Query queryForSubDoc = new Query();
                for (ObjectId id : finalDocIds) {
                    queryForSubDoc.addCriteria(Criteria.where(Consts.OBJECT_ID).is(id));
                }
                Map<String, Object> childrenMap = new HashMap<>();
                childrenMap.put(Consts.CHILDREN, refIds);
                updateWhenSetKeyDone(queryForSubDoc, new ArrayList<>(), childrenMap);

                // 最后同步更新app结构
                Query query = new Query(Criteria.where(Consts.APP_ID).is(appId));
                List<SegmentPath> paths = pathConverter.convert(outerPath);
                updateWhenSetKeyDone(query, paths, childrenMap);
            } else {
                commonField.put(key, object.get(key));
            }
        }
        if (nameExist) {
            // 先更新子文档的name
            Query queryForSubDoc = new Query();
            List<ObjectId> targetDocIds = getFinalDocs(outerPath);
            for (ObjectId targetDocId : targetDocIds) {
                queryForSubDoc.addCriteria(Criteria.where(Consts.OBJECT_ID).is(targetDocId));
            }
            Map<String, Object> nameMap = new HashMap<>();
            nameMap.put("name", object.get("name"));
            updateWhenSetKeyDone(queryForSubDoc, new ArrayList<>(), nameMap);
            // 最后同步更新app中的name
            Query query = new Query(Criteria.where(Consts.APP_ID).is(appId));
            List<SegmentPath> paths = pathConverter.convert(outerPath);
            updateWhenSetKeyDone(query, paths, nameMap);
        }

        // 更新正常字段
        if (commonField.size() > 0) {
            Query queryForSubDoc = new Query();
            List<ObjectId> finalDocIds = getFinalDocs(outerPath);
            for (ObjectId id : finalDocIds) {
                queryForSubDoc.addCriteria(Criteria.where(Consts.OBJECT_ID).is(id));
            }
            updateWhenSetKeyDone(queryForSubDoc, new ArrayList<>(), commonField);
        }
    }

    /**
     * @description: 在构建的每个setKey末尾加上要修改的key，然后修改其value
     * @return:
     */
    private void updateWhenSetKeyDone(Query query, List<SegmentPath> paths, Map<String, Object> object) {
        Update update = new Update();
        List<String> setKeys = repoUtil.getSetKeys(paths, paths.size(), update);
        for (String key : object.keySet()) {
            // 给setKey加上要修改的字段
            for (String setKey : setKeys) {
                String finalSetKey = setKey.isEmpty() ? key : setKey + "." + key;
                update.set(finalSetKey, object.get(key));
            }
        }
        UpdateResult app = mongoTemplate.updateFirst(query, update, Consts.COLLECTION_NAME);
        printUpdateResult(app);
    }

    private List<Map> getViews(String appId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where(Consts.APP_ID).is(appId)),
                Aggregation.unwind(Consts.VIEWS),
                Aggregation.replaceRoot(Consts.VIEWS)
        );
        AggregationResults<Map> app = mongoTemplate.aggregate(aggregation, "app", Map.class);
        List<Map> ret = new ArrayList<>();
        Iterator<Map> it = app.iterator();
        while (it.hasNext()) {
            ret.add(it.next());
        }
        return ret;
    }

    private List<Map> getLogics(String appId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where(Consts.APP_ID).is(appId)),
                Aggregation.unwind(Consts.LOGICS),
                Aggregation.replaceRoot(Consts.LOGICS)
        );
        AggregationResults<Map> app = mongoTemplate.aggregate(aggregation, "app", Map.class);
        List<Map> ret = new ArrayList<>();
        Iterator<Map> it = app.iterator();
        while (it.hasNext()) {
            ret.add(it.next());
        }
        return ret;
    }


    /**
     * 不存在外部路径
     * 特殊情况：删除app.views、删除app.logics
     * 路径用例："path": "app.elements[0]"
     */
    private void deleteWhenOuterEmpty(String appId, String innerPath) {
        if (Consts.VIEWS.equals(innerPath)) {
            repoUtil.deleteViews(getViews(appId));
        } else if (Consts.LOGICS.equals(innerPath)) {
            repoUtil.deleteLogics(getLogics(appId));
        }
        Query query = new Query(Criteria.where(Consts.APP_ID).is(appId));
        List<SegmentPath> paths = pathConverter.convert(innerPath);
        deleteByLastPathType(query, paths);
    }


    /**
     * 存在外部路径，存在内部路径
     * 特殊情况：删除view的children
     * 路径用例："path": "app.views[1:3].children"
     */
    private void deleteWhenInnerExist(String appId, String outerPath, String innerPath) {
        // 注意：logics或其他元素也可能有children字段
        if (outerPath.contains(Consts.VIEWS) && Consts.CHILDREN.equals(innerPath)) {
            // 查询子文档children并删除
            String queryPath = Consts.APP + "." + outerPath;
            Map<String, Object> view = (Map<String, Object>) splitUtil.get(null, queryPath, new ArrayList<>());
            repoUtil.deleteViews((List<Map>) view.get(Consts.CHILDREN));
            // 同步修改app结构
            Query query = new Query(Criteria.where(Consts.APP_ID).is(appId));
            String rawPath = outerPath + "." + innerPath;
            List<SegmentPath> paths = pathConverter.convert(rawPath);
            deleteByLastPathType(query, paths);
        }
        // 同步删除view子文档中的children 或者 删除普通字段
        Query query = new Query();
        List<ObjectId> finalDocIds = getFinalDocs(outerPath);
        for (ObjectId oid : finalDocIds) {
            query.addCriteria(Criteria.where(Consts.OBJECT_ID).is(oid));
        }
        List<SegmentPath> paths = pathConverter.convert(innerPath);
        deleteByLastPathType(query, paths);
    }


    /**
     * 存在外部路径，不存在内部路径
     * 特殊情况：删除view、删除logic
     * 路径用例："path": "app.views[3]"
     */
    private void deleteWhenInnerEmpty(String appId, String outerPath) {
        // 先删除选中的view或者logic
        String rawPath = Consts.APP + "." + outerPath;
        if (outerPath.contains(Consts.VIEWS)) {
            Object o = splitUtil.get(null, rawPath, new ArrayList<>());
            repoUtil.deleteView((Map) o);
        } else if (outerPath.contains(Consts.LOGICS)) {
            Object o = splitUtil.get(null, rawPath, new ArrayList<>());
            repoUtil.deleteLogic((Map) o);
        }

        // 在删除app中对应的结构
        Query query = new Query(Criteria.where(Consts.APP_ID).is(appId));
        List<SegmentPath> paths = pathConverter.convert(rawPath);
        deleteByLastPathType(query, paths);
    }


    /**
     * @description: mongodb只有按kv条件删除，所以删除字段、KV删除数组元素、索引删除数组元素三种实现不一样
     * @return:
     */
    private void deleteByLastPathType(Query query, List<SegmentPath> paths) {
        SegmentPath.SegmentPathType lastPathType = paths.get(paths.size() - 1).getType();
        if (SegmentPath.SegmentPathType.field == lastPathType) {
            deleteByField(query, paths);
        } else if (SegmentPath.SegmentPathType.kv == lastPathType) {
            deleteByKv(query, paths);
        } else {
            deleteByIdxOrRange(query, paths);
        }
    }

    /**
     * @description: 删除字段，直接unset即可
     * @return:
     */
    private void deleteByField(Query query, List<SegmentPath> paths) {
        Update update = new Update();
        List<String> setKeys = repoUtil.getSetKeys(paths, paths.size(), update);
        for (String setKey : setKeys) {
            update.unset(setKey);
        }
        UpdateResult app = mongoTemplate.updateFirst(query, update, Consts.COLLECTION_NAME);
        printUpdateResult(app);
    }

    /**
     * @description: 根据kv删除对象
     * @return:
     */
    private void deleteByKv(Query query, List<SegmentPath> paths) {
        Update update = new Update();
        // kv形式删除对象，首先去掉setKey的最后一个[...]，把里面的kv作为condition
        List<String> setKeys = repoUtil.getSetKeys(paths, paths.size() - 1, update);
        // 把paths最后一个取出来作为kv的条件
        KvPath lastKvPath = (KvPath) paths.get(paths.size() - 1);
        for (int i = 0; i < setKeys.size(); i++) {
            String s = setKeys.get(i);
            s = s.isEmpty() ? "" : s + ".";
            s += lastKvPath.getPath();
            setKeys.set(i, s);
        }
        String key = lastKvPath.getKey(), value = lastKvPath.getValue();
        for (String setKey : setKeys) {
            update.pull(setKey, new BasicDBObject(key, value));
        }
        UpdateResult app = mongoTemplate.updateFirst(query, update, Consts.COLLECTION_NAME);
        printUpdateResult(app);
    }

    /**
     * @description: 根据索引删除对象，mongo不直接支持
     * 这里选择先给要删除的对象加上 deleted:true 字段，再根据此条件删除，需要两次IO
     * todo 暂未找到更佳方案
     * @return:
     */
    private void deleteByIdxOrRange(Query query, List<SegmentPath> paths) {
        Update addTagUpdate = new Update();
        List<String> addTagSetKeys = repoUtil.getSetKeys(paths, paths.size(), addTagUpdate);
        for (int i = 0; i < addTagSetKeys.size(); i++) {
            String addTagSetKey = addTagSetKeys.get(i);
            addTagSetKey += ".deleted";
            addTagUpdate.set(addTagSetKey, "true");
        }
        UpdateResult updateResult = mongoTemplate.updateFirst(query, addTagUpdate, Consts.COLLECTION_NAME);
        printUpdateResult(updateResult);

        Update update = new Update();
        List<String> deleteSetKeys = repoUtil.getSetKeys(paths, paths.size() - 1, update);

        String arrName = "";
        SegmentPath lastPath = paths.get(paths.size() - 1);
        if (lastPath.getType() == SegmentPath.SegmentPathType.idx || lastPath.getType() == SegmentPath.SegmentPathType.range) {
            arrName = lastPath.getPath();
        }

        for (String deleteSetKey : deleteSetKeys) {
            deleteSetKey += deleteSetKey.isEmpty() ? "" : ".";
            deleteSetKey += arrName;//获得最后一个path的arrName
            update.pull(deleteSetKey, new BasicDBObject("deleted", "true"));
        }
        UpdateResult res = mongoTemplate.updateFirst(query, update, Consts.COLLECTION_NAME);
        printUpdateResult(res);
    }

    /**
     * @description: 输入外部路径，获得其所有最终文档oid
     * @return:
     */
    private List<ObjectId> getFinalDocs(String outerPath) {
        outerPath = Consts.APP + "." + outerPath;
        List<SegmentPath> convert = pathConverter.convert(outerPath);
        LocationDocument location = splitUtil.locationDoc(convert);
        return location.getObjectIds();
    }

    private void printUpdateResult(UpdateResult app) {
        log.info("match:" + app.getMatchedCount() + " modified:" + app.getModifiedCount());
    }

}