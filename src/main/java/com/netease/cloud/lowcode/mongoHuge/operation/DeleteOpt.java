package com.netease.cloud.lowcode.mongoHuge.operation;

import com.mongodb.BasicDBObject;
import com.mongodb.client.result.UpdateResult;
import com.netease.cloud.lowcode.mongoHuge.common.Consts;
import com.netease.cloud.lowcode.mongoHuge.pathEntity.KvPath;
import com.netease.cloud.lowcode.mongoHuge.pathEntity.SegPath;
import com.netease.cloud.lowcode.mongoHuge.pathEntity.QueryPath;
import com.netease.cloud.lowcode.mongoHuge.splitSchema.SplitQuery;
import com.netease.cloud.lowcode.mongoHuge.splitSchema.SplitSchemaConfig;
import com.netease.cloud.lowcode.mongoHuge.util.MongoUtil;
import com.netease.cloud.lowcode.mongoHuge.util.SpringUtil;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/7/5 14:57
 */

public class DeleteOpt {

    private List<ObjectId> finalOids;

    private QueryPath queryPath;

    private List<SegPath> innerPath;

    private List<SegPath> outerPath;

    private String mongoKey;

    private SegPath lastPath;


    public MongoTemplate getMongoTemplate() {
        return (MongoTemplate) SpringUtil.getBean(MongoTemplate.class);
    }


    // 指定路径删除
    public void delete(QueryPath queryPath, SegPath lastPath) {
        this.queryPath = queryPath;
        this.finalOids = queryPath.initOuterPath();
        this.innerPath = queryPath.getInner();
        this.outerPath = queryPath.getOuter();
        this.lastPath = lastPath;
        this.mongoKey = queryPath.getMongoKey();

        if (outerPath.size() > 0 && innerPath.size() > 0) {
            // 存在外部路径 + 存在内部 == 从更新
            deleteWhenInnerExist();
        } else if (outerPath.size() > 0) {
            // 只存在外部路径 == splitTarget主删除 + 普通字段子删除(这里假设不会删除连接字段)
            deleteWhenInnerEmpty();
        } else {
            // 只存在内部路径 == splitTarget主删除 + 普通字段主删除
            deleteWhenOuterEmpty();
        }
    }

    private void deleteWhenInnerExist() {
        Query queryForSub = new Query();
        finalOids.forEach(id -> queryForSub.addCriteria(Criteria.where(Consts.OBJECT_ID).is(id)));
        innerPath.add(lastPath);
        deleteByLastPathType(queryForSub, innerPath);
    }

    private void deleteWhenInnerEmpty() {
        Map<String, SplitQuery> splitTarget = SplitSchemaConfig.recurFields;
        String arrName = lastPath.getPath();
        // 第一层包含了拆分目标
        outerPath.add(lastPath);
        if (splitTarget.containsKey(arrName)) {
            // 主删除子结构
            InitOpt.deleteJson(QueryOpt.findWithOutBeautify(queryPath));
            Query queryForTree = new Query(Criteria.where(Consts.MONGO_KEY).is(mongoKey));
            deleteByLastPathType(queryForTree, outerPath);
        } else {
            // 子文档删除普通字段
            Query queryForSub = new Query();
            finalOids.forEach(id -> queryForSub.addCriteria(Criteria.where(Consts.OBJECT_ID).is(id)));
            List<SegPath> paths = Collections.singletonList(lastPath);
            deleteByLastPathType(queryForSub, paths);
        }
    }


    private void deleteWhenOuterEmpty() {
        Map<String, SplitQuery> splitTarget = SplitSchemaConfig.splitTargets;
        String arrName = lastPath.getPath();
        // 第一层包含了拆分目标
        innerPath.add(lastPath);
        if (splitTarget.containsKey(arrName)) {
            // 主删除子结构
            InitOpt.deleteJson(QueryOpt.findWithOutBeautify(queryPath));
        }

        Query queryForTree = new Query(Criteria.where(Consts.MONGO_KEY).is(mongoKey));
        deleteByLastPathType(queryForTree, innerPath);
    }


    /**
     * @description: mongodb只有按kv条件删除，所以删除字段、KV删除数组元素、索引删除数组元素三种实现不一样
     * @return:
     */
    private void deleteByLastPathType(Query query, List<SegPath> paths) {
        SegPath.Type lastPathType = paths.get(paths.size() - 1).getType();
        if (SegPath.Type.field == lastPathType) {
            deleteByField(query, paths);
        } else if (SegPath.Type.kv == lastPathType) {
            deleteByKv(query, paths);
        } else {
            deleteByIdxOrRange(query, paths);
        }
    }

    /**
     * @description: 删除字段，直接unset即可
     * @return:
     */
    private void deleteByField(Query query, List<SegPath> paths) {
        Update update = new Update();
        List<String> setKeys = MongoUtil.getSetKeys(paths, paths.size(), update);
        for (String setKey : setKeys) {
            update.unset(setKey);
        }
        UpdateResult app = getMongoTemplate().updateFirst(query, update, Consts.COLLECTION_NAME);
        MongoUtil.printUpdateResult(app);
    }

    /**
     * @description: 根据kv删除对象
     * @return:
     */
    private void deleteByKv(Query query, List<SegPath> paths) {
        Update update = new Update();
        // kv形式删除对象，首先去掉setKey的最后一个[...]，把里面的kv作为condition
        List<String> setKeys = MongoUtil.getSetKeys(paths, paths.size() - 1, update);
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
        UpdateResult app = getMongoTemplate().updateFirst(query, update, Consts.COLLECTION_NAME);
        MongoUtil.printUpdateResult(app);
    }

    /**
     * @description: 根据索引删除对象，mongo不直接支持
     * 这里选择先给要删除的对象加上 deleted:true 字段，再根据此条件删除，需要两次IO
     * todo 暂未找到更佳方案
     * @return:
     */
    private void deleteByIdxOrRange(Query query, List<SegPath> paths) {
        Update addTagUpdate = new Update();
        List<String> addTagSetKeys = MongoUtil.getSetKeys(paths, paths.size(), addTagUpdate);
        for (int i = 0; i < addTagSetKeys.size(); i++) {
            String addTagSetKey = addTagSetKeys.get(i);
            addTagSetKey += ".deleted";
            addTagUpdate.set(addTagSetKey, "true");
        }
        UpdateResult updateResult = getMongoTemplate().updateFirst(query, addTagUpdate, Consts.COLLECTION_NAME);
        MongoUtil.printUpdateResult(updateResult);

        Update update = new Update();
        List<String> deleteSetKeys = MongoUtil.getSetKeys(paths, paths.size() - 1, update);

        String arrName = "";
        SegPath lastPath = paths.get(paths.size() - 1);
        if (lastPath.getType() == SegPath.Type.idx || lastPath.getType() == SegPath.Type.range) {
            arrName = lastPath.getPath();
        }

        for (String deleteSetKey : deleteSetKeys) {
            deleteSetKey += deleteSetKey.isEmpty() ? "" : Consts.DOT;
            deleteSetKey += arrName;//获得最后一个path的arrName
            update.pull(deleteSetKey, new BasicDBObject("deleted", "true"));
        }
        UpdateResult res = getMongoTemplate().updateFirst(query, update, Consts.COLLECTION_NAME);
        MongoUtil.printUpdateResult(res);
    }

}
































