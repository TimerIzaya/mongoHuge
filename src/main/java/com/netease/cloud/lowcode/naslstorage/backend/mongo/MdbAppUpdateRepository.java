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
    private MdbReferenceDocumentUpdateRepository storeUtil;

    @Resource(name = "mdbPathConverter")
    private PathConverter pathConverter;

    @Resource(name = "splitMdbAppRepositoryImpl")
    private MdbSplitQueryRepository queryUtil;

    public void initApp(Map<String, Object> object) {
        // views要先判断是否是数组, 防止json不符合规范
        Object views = object.get(Consts.VIEWS);
        if (views instanceof ArrayList) {
            List<Map> saveViews = storeUtil.saveViews((List<Map>) views);
            object.put(Consts.VIEWS, saveViews);
        }
        Object logics = object.get(Consts.LOGICS);
        if (logics instanceof ArrayList) {
            List<Map> saveLogics = storeUtil.saveLogics((List<Map>) logics);
            object.put(Consts.LOGICS, saveLogics);
        }
        // 前端要求增加时间戳
        object.put(Consts.TIMESTAMP, System.currentTimeMillis());
        storeUtil.insertDocument(object);
    }


    /**
     * @description: 在目标数组指定位置添加对象
     * @return:
     */
    public void create(String outerPath, String innerPath, Map<String, Object> object) {
        String appId = AppIdContext.get();
        // 前端要求增加时间戳
        object.put(Consts.TIMESTAMP, System.currentTimeMillis());
        object.put(Consts.UPDATE_BY_APP, appId);
        if (outerPath.isEmpty()) {
            createWhenOuterEmpty(appId, innerPath, object);
        } else if (!innerPath.isEmpty()) {
            createWhenInnerExist(outerPath, innerPath, object);
        } else {
            createWhenInnerEmpty(outerPath, object);
        }
    }


    public void deleteApp(String appId) {
        // 获得当前app的views和logics并删除
        storeUtil.deleteViews(getViews());
        storeUtil.deleteLogics(getLogics());
        // 删除app整体
        mongoTemplate.remove(new Query(Criteria.where(Consts.APP_ID).is(appId)), Consts.COLLECTION_NAME);
    }


    /**
     * 更新对象的字段
     */
    public void update(String outerPath, String innerPath, Map<String, Object> object) {
        String appId = AppIdContext.get();
        // 前端要求增加时间戳
        object.put(Consts.TIMESTAMP, System.currentTimeMillis());
        object.put(Consts.UPDATE_BY_APP, appId);
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
            deleteWhenOuterEmpty(appId, innerPath);
        } else if (!innerPath.isEmpty()) {
            deleteWhenInnerExist(appId, outerPath, innerPath);
        } else {
            deleteWhenInnerEmpty(appId, outerPath);
        }
    }


    /**
     * 不存在外部路径 = 修改app + 保存子文档
     * 路径用例："path": "app.processes[2].properties[6]",
     * 路径用例："path": "app.views"
     * 路径用例："path": "app.logics"
     */
    private void createWhenOuterEmpty(String appId, String innerPath, Map<String, Object> object) {
        boolean isView = object.containsKey(Consts.CONCEPT) && object.get(Consts.CONCEPT).equals(Consts.CONCEPT_VIEW);
        boolean isLogic = object.containsKey(Consts.CONCEPT) && object.get(Consts.CONCEPT).equals(Consts.CONCEPT_LOGIC);
        if (isView) {
            object = storeUtil.saveView(object);
        }
        if (isLogic) {
            object = storeUtil.saveLogic(object);
        }

        List<SegmentPath> paths = pathConverter.convert(innerPath);
        Query query = new Query(Criteria.where(Consts.APP_ID).is(appId));
        createAtDoc(query, paths, object);
    }


    /**
     * 存在外部路径 + 存在内部路径
     * 路径用例："path": "app.logics[2].properties[6]"
     * 路径用例："path": "app.views[2].children[3].children[4].children" （特殊情况）
     * 路径用例："path": "app.views[2].logics"
     * 路径用例："path": "app.views[2].elements"
     */
    private void createWhenInnerExist(String outerPath, String innerPath, Map<String, Object> object) {
        boolean isView = object.containsKey(Consts.CONCEPT) && object.get(Consts.CONCEPT).equals(Consts.CONCEPT_VIEW);

        if (innerPath.equals(Consts.CHILDREN) && isView) {
            // 特殊情况，object是view 并且 inner是children，需要同步更新
            object = storeUtil.saveView(object);
            String rawPath = outerPath + Consts.DOT + innerPath;
            List<SegmentPath> rawPaths = pathConverter.convert(rawPath);
            Query query = new Query(Criteria.where(Consts.APP_ID).is(AppIdContext.get()));
            createAtDoc(query, rawPaths, object);
        } else {
            // 一般情况
            List<ObjectId> targetDocIds = getFinalDocs(outerPath);
            Query querySubDoc = new Query();
            for (ObjectId targetDocId : targetDocIds) {
                querySubDoc.addCriteria(Criteria.where(Consts.OBJECT_ID).is(targetDocId));
            }
            List<SegmentPath> innerPaths = pathConverter.convert(innerPath);
            createAtDoc(querySubDoc, innerPaths, object);
        }
    }


    /**
     * 存在外部路径 + 不存在内部路径 = 修改app + 保存子文档
     * 路径用例: "path": "app.views[0].children[1].children[2].children[3]"
     * 只需要修改app结构
     */
    private void createWhenInnerEmpty(String outerPath, Map<String, Object> object) {
        boolean isView = object.containsKey(Consts.CONCEPT) && object.get(Consts.CONCEPT).equals(Consts.CONCEPT_VIEW);
        boolean isLogic = object.containsKey(Consts.CONCEPT) && object.get(Consts.CONCEPT).equals(Consts.CONCEPT_LOGIC);
        if (isView) {
            object = storeUtil.saveView(object);
        }
        if (isLogic) {
            object = storeUtil.saveLogic(object);
        }

        List<SegmentPath> rawPaths = pathConverter.convert(outerPath);
        Query query = new Query(Criteria.where(Consts.APP_ID).is(AppIdContext.get()));
        createAtDoc(query, rawPaths, object);
    }


    /**
     * @description: find最终要create的文档，在数组指定位置添加对象，
     * @return:
     */
    private void createAtDoc(Query query, List<SegmentPath> paths, Map<String, Object> object) {
        Update update = new Update();
        SegmentPath lastPath = paths.get(paths.size() - 1);

        List<String> setKeys = storeUtil.getSetKeys(paths, paths.size() - 1, update);
        for (String setKey : setKeys) {
            String finalSetKey = setKey.isEmpty() ? "" : setKey + Consts.DOT;
            if (lastPath.getType().equals(SegmentPath.SegmentPathType.field)) {
                // 最后一个path是field，特殊处理，默认append到数组最后一位
                finalSetKey += lastPath.getPath();
                update.push(finalSetKey, object);
            } else {
                // 最后一个path是数组和索引，不用解析到setKey
                finalSetKey += lastPath.getPath(); // 根据lastPath确定要create的数组
                int idx = ((IdxPath) lastPath).getIdx(); // // 根据lastPath确定要create的数组位置
                update.push(finalSetKey).atPosition(idx).value(object);
            }
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
                storeUtil.deleteViews(getViews());
                // 保存新views数组
                Object views = object.get(key);
                if (views instanceof ArrayList) {
                    List<Map> refIds = storeUtil.saveViews((List<Map>) views);
                    object.put(key, refIds);
                }
            } else if (Consts.LOGICS.equals(key)) {
                // 删除旧logics数组
                storeUtil.deleteLogics(getLogics());
                // 保存新logics数组
                Object logics = object.get(key);
                if (logics instanceof ArrayList) {
                    List<Map> refIds = storeUtil.saveLogics((List<Map>) logics);
                    object.put(key, refIds);
                }
            }
        }
        updateWhenSetKeyDone(query, paths, object);
    }


    /**
     * 存在外部路径 + 存在内部路径 = 修改目标文档
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
     * 存在外部路径 + 不存在内部路径
     * 特殊情况：更新name、更新children
     * 注意！！！更新name需要放在最后更新，防止paths的lastPath是跟据name定位的
     * 路径用例："path": "app.views[2].children[3]" -> {name: xxx, children: {[],[]}, type: xxx}
     */
    private void updateWhenInnerEmpty(String appId, String outerPath, Map<String, Object> object) {
        // 保留非特殊字段，批量修改
        Map<String, Object> commonField = new HashMap<>();
        boolean nameExist = false;
        for (String key : object.keySet()) {
            if (Consts.NAME.equals(key)) {
                // 修改name字段 最后执行
                nameExist = true;
            } else if (Consts.CHILDREN.equals(key)) {
                // 先删除children里所有view
                String rawPath = Consts.APP + Consts.DOT + outerPath;
                Map<String, Object> finalDoc = (Map<String, Object>) queryUtil.get(null, rawPath, new ArrayList<>());
                storeUtil.deleteViews((List<Map>) finalDoc.get(Consts.CHILDREN));

                // 再根据object生成children
                Object children = object.get(key);
                List<Map> refIds = null;
                if (children instanceof ArrayList) {
                    List<Map> saveViews = (List<Map>) object.get(key);
                    refIds = storeUtil.saveViews(saveViews);
                }

                Map<String, Object> childrenMap = new HashMap<>();
                childrenMap.put(Consts.CHILDREN, refIds);

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
            nameMap.put(Consts.NAME, object.get(Consts.NAME));
            updateWhenSetKeyDone(queryForSubDoc, new ArrayList<>(), nameMap);
            // 最后同步更新app中的name
            Query query = new Query(Criteria.where(Consts.APP_ID).is(appId));
            List<SegmentPath> paths = pathConverter.convert(outerPath);
            updateWhenSetKeyDone(query, paths, nameMap);
        }

        // 更新正常字段
        if (commonField.size() > 0) {
            Query querySubDoc = new Query();
            List<ObjectId> finalDocIds = getFinalDocs(outerPath);
            for (ObjectId id : finalDocIds) {
                querySubDoc.addCriteria(Criteria.where(Consts.OBJECT_ID).is(id));
            }
            updateWhenSetKeyDone(querySubDoc, new ArrayList<>(), commonField);
        }
    }

    /**
     * @description: 在构建的每个setKey末尾加上要修改的key，然后修改其value
     * @return:
     */
    private void updateWhenSetKeyDone(Query query, List<SegmentPath> paths, Map<String, Object> object) {
        Update update = new Update();
        List<String> setKeys = storeUtil.getSetKeys(paths, paths.size(), update);
        for (String key : object.keySet()) {
            // 给setKey加上要修改的字段
            for (String setKey : setKeys) {
                String finalSetKey = setKey.isEmpty() ? key : setKey + Consts.DOT + key;
                update.set(finalSetKey, object.get(key));
            }
        }
        UpdateResult app = mongoTemplate.updateFirst(query, update, Consts.COLLECTION_NAME);
        printUpdateResult(app);
    }

    /**
     * @description: 获得当前app的views字段，如果为空则返回空链表
     * @return:
     */
    private List<Map> getViews() {
        String appViewsPath = Consts.APP + Consts.DOT + Consts.VIEWS;
        Object views = queryUtil.get(null, appViewsPath, new ArrayList<>());
        if (views instanceof ArrayList) {
            return (List<Map>) views;
        }
        return new ArrayList<>();
    }

    /**
     * @description: 获得当前app的logics字段，如果为空则返回空链表
     * @return:
     */
    private List<Map> getLogics() {
        String appLogicsPath = Consts.APP + Consts.DOT + Consts.LOGICS;
        Object logics = queryUtil.get(null, appLogicsPath, new ArrayList<>());
        if (logics instanceof ArrayList) {
            return (List<Map>) logics;
        }
        return new ArrayList<>();
    }


    /**
     * 不存在外部路径
     * 特殊情况：删除app.views、删除app.logics
     * 路径用例："path": "app.elements[0]"
     */
    private void deleteWhenOuterEmpty(String appId, String innerPath) {
        if (Consts.VIEWS.equals(innerPath)) {
            storeUtil.deleteViews(getViews());
        } else if (Consts.LOGICS.equals(innerPath)) {
            storeUtil.deleteLogics(getLogics());
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
            String queryPath = Consts.APP + Consts.DOT + outerPath;
            Map<String, Object> view = (Map<String, Object>) queryUtil.get(null, queryPath, new ArrayList<>());
            storeUtil.deleteViews((List<Map>) view.get(Consts.CHILDREN));
            // 同步修改app结构
            Query query = new Query(Criteria.where(Consts.APP_ID).is(appId));
            String rawPath = outerPath + Consts.DOT + innerPath;
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
        String rawPath = Consts.APP + Consts.DOT + outerPath;
        if (outerPath.contains(Consts.VIEWS)) {
            Object o = queryUtil.get(null, rawPath, new ArrayList<>());
            storeUtil.deleteView((Map) o);
        } else if (outerPath.contains(Consts.LOGICS)) {
            Object o = queryUtil.get(null, rawPath, new ArrayList<>());
            storeUtil.deleteLogic((Map) o);
        }

        // 在删除app中对应的结构
        Query query = new Query(Criteria.where(Consts.APP_ID).is(appId));
        List<SegmentPath> paths = pathConverter.convert(outerPath);
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
        List<String> setKeys = storeUtil.getSetKeys(paths, paths.size(), update);
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
        List<String> setKeys = storeUtil.getSetKeys(paths, paths.size() - 1, update);
        // 把paths最后一个取出来作为kv的条件
        KvPath lastKvPath = (KvPath) paths.get(paths.size() - 1);
        for (int i = 0; i < setKeys.size(); i++) {
            String s = setKeys.get(i);
            s = s.isEmpty() ? "" : s + Consts.DOT;
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
        List<String> addTagSetKeys = storeUtil.getSetKeys(paths, paths.size(), addTagUpdate);
        for (int i = 0; i < addTagSetKeys.size(); i++) {
            String addTagSetKey = addTagSetKeys.get(i);
            addTagSetKey += ".deleted";
            addTagUpdate.set(addTagSetKey, "true");
        }
        UpdateResult updateResult = mongoTemplate.updateFirst(query, addTagUpdate, Consts.COLLECTION_NAME);
        printUpdateResult(updateResult);

        Update update = new Update();
        List<String> deleteSetKeys = storeUtil.getSetKeys(paths, paths.size() - 1, update);

        String arrName = "";
        SegmentPath lastPath = paths.get(paths.size() - 1);
        if (lastPath.getType() == SegmentPath.SegmentPathType.idx || lastPath.getType() == SegmentPath.SegmentPathType.range) {
            arrName = lastPath.getPath();
        }

        for (String deleteSetKey : deleteSetKeys) {
            deleteSetKey += deleteSetKey.isEmpty() ? "" : Consts.DOT;
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
        outerPath = Consts.APP + Consts.DOT + outerPath;
        List<SegmentPath> convert = pathConverter.convert(outerPath);
        LocationDocument location = queryUtil.locationDoc(convert, new ArrayList<>());
        return location.getObjectIds();
    }

    private void printUpdateResult(UpdateResult app) {
        log.info("match:" + app.getMatchedCount() + " modified:" + app.getModifiedCount());
    }
}