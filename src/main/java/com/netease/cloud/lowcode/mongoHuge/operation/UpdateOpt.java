package com.netease.cloud.lowcode.mongoHuge.operation;

import com.mongodb.client.result.UpdateResult;
import com.netease.cloud.lowcode.mongoHuge.common.Consts;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * todo 优先考虑当前nasl-storage开发进度，当前update只适用于KVPath中的K是JointField的情况
 */
public class UpdateOpt {

    private Object obj;

    private List<ObjectId> finalOids;

    private QueryPath queryPath;

    private List<SegPath> innerPath;

    private List<SegPath> outerPath;

    private String mongoKey;

    public MongoTemplate getMongoTemplate() {
        return (MongoTemplate) SpringUtil.getBean(MongoTemplate.class);
    }

    public void update(QueryPath queryPath, Object obj) {
        this.finalOids = queryPath.initOuterPath();
        this.queryPath = queryPath;
        this.innerPath = queryPath.getInner();
        this.outerPath = queryPath.getOuter();
        this.mongoKey = queryPath.getMongoKey();
        this.obj = obj;

        // 存在外部路径 + 存在内部 == 从更新
        if (outerPath.size() > 0 && innerPath.size() > 0) {
            updateWhenInnerExist();
        } else if (outerPath.size() > 0) {
            // 只存在外部路径 == JointField主从更新 + RecurField主更新 + 普通字段从更新
            updateWhenInnerEmpty();
        } else {
            // 只存在内部路径 == RecurField主更新 + 普通字段主更新
            updateWhenOuterEmpty();
        }
    }

    private void updateWhenInnerExist() {
        Query query = new Query();
        for (ObjectId targetDocId : finalOids) {
            query.addCriteria(Criteria.where(Consts.OBJECT_ID).is(targetDocId));
        }
        updateWhenSetKeyDone(query, innerPath, obj);
    }


    private void updateWhenInnerEmpty() {
        Map<String, Object> map = (Map) obj;
        Map updateObj = (Map) QueryOpt.findWithOutBeautify(queryPath);


        // todo 这个循环写的很蠢，要改，不要让SplitSchema影响CURD
        for (SplitQuery splitQuery : SplitSchemaConfig.splitQueries) {
            List<String> jointField = splitQuery.getJointFields();
            String recurField = splitQuery.getRecurField();
            // 保留普通字段，批量修改
            Map<String, Object> commonMap = new HashMap<>();
            // 保留连接字段，同步主体Json结构，最后更新
            Map<String, Object> jointMap = new HashMap<>();
            // 保留递归字段，同步主体Json结构
            Map<String, Object> recurMap = new HashMap<>();
            for (String k : map.keySet()) {
                Object v = map.get(k);
                if (jointField.contains(k)) {
                    jointMap.put(k, v);
                } else if (recurField != null && recurField.equals(k)) {
                    recurMap.put(k, v);
                } else {
                    commonMap.put(k, v);
                }
            }

            Query queryForTree = new Query(Criteria.where(Consts.MONGO_KEY).is(mongoKey));
            Query queryForSub = new Query();
            finalOids.forEach(id -> queryForSub.addCriteria(Criteria.where(Consts.OBJECT_ID).is(id)));

            // 处理递归字段, 更新主体Json结构
            if (recurMap.size() != 0) {
                map.remove(recurField);
                // 先找到旧结构删除
                Object oldSub = updateObj.get(recurField);
                InitOpt.deleteJson(oldSub);
                // 再保存新结构
                Object subStructure = InitOpt.detachJson(recurMap.get(recurField), splitQuery);
                recurMap.put(recurField, subStructure);
                updateWhenSetKeyDone(queryForTree, outerPath, recurMap);
            }
            // 处理连接字段，主体Json结构和子文档都要更新
            if (jointMap.size() != 0) {
                jointField.forEach(map::remove);
                updateWhenSetKeyDone(queryForTree, outerPath, jointMap);
                updateWhenSetKeyDone(queryForSub, new ArrayList<>(), jointMap);
            }
            // 批量处理普通字段，更新子文档
            updateWhenSetKeyDone(queryForSub, new ArrayList<>(), commonMap);
        }
    }


    private void updateWhenOuterEmpty() {
        // 如果拆分路径只有一层, 那么主更新要注意此字段
        Map<String, SplitQuery> concernField = SplitSchemaConfig.firstLevelTargets;

        Map<String, Object> map = (Map) obj;
        Map updateObj = (Map) QueryOpt.findWithOutBeautify(queryPath);
        // 把第一层就拆分的对象分离出去, 比如obj:{views:[]}
        for (String k : map.keySet()) {
            Object v = map.get(k);
            if (concernField.containsKey(k)) {
                // 先找到旧结构删除
                Object oldSub = updateObj.get(k);
                InitOpt.deleteJson(oldSub);
                // 再保存新结构
                Object subStructure = InitOpt.detachJson(v, concernField.get(k));
                map.put(k, subStructure);
            }
        }

        Query queryForTree = new Query(Criteria.where(Consts.MONGO_KEY).is(mongoKey));
        updateWhenSetKeyDone(queryForTree, new ArrayList<>(), map);
    }

    /**
     * 在segPath确定的情况下
     * 在构建的每个setKey末尾加上要修改的key，然后修改其value
     */
    private void updateWhenSetKeyDone(Query query, List<SegPath> paths, Object obj) {
        Map<String, Object> map = (Map<String, Object>) obj;
        Update update = new Update();
        List<String> setKeys = MongoUtil.getSetKeys(paths, paths.size(), update);
        for (String key : map.keySet()) {
            // 给setKey加上要修改的字段
            for (String setKey : setKeys) {
                String finalSetKey = setKey.isEmpty() ? key : setKey + Consts.DOT + key;
                update.set(finalSetKey, map.get(key));
            }
        }
        UpdateResult app = getMongoTemplate().updateFirst(query, update, Consts.COLLECTION_NAME);
    }


}
