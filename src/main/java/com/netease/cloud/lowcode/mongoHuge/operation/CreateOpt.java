package com.netease.cloud.lowcode.mongoHuge.operation;

import com.mongodb.client.result.UpdateResult;
import com.netease.cloud.lowcode.mongoHuge.common.Consts;
import com.netease.cloud.lowcode.mongoHuge.pathEntity.IdxPath;
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
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/7/5 11:31
 */

public class CreateOpt {

    private Object obj;

    private List<ObjectId> finalOids;

    private List<SegPath> innerPath;

    private List<SegPath> outerPath;

    private String mongoKey;

    private SegPath lastPath;

    public MongoTemplate getMongoTemplate() {
        return (MongoTemplate) SpringUtil.getBean(MongoTemplate.class);
    }


    /**
     * queryPath定位到指定对象，arrName为对象字段
     */
    public void create(QueryPath queryPath, SegPath lastPath, Object obj) {
        this.finalOids = queryPath.initOuterPath();
        this.innerPath = queryPath.getInner();
        this.outerPath = queryPath.getOuter();
        this.lastPath = lastPath;
        this.mongoKey = queryPath.getMongoKey();
        this.obj = obj;

        // 存在外部路径 + 存在内部 == 从更新
        if (outerPath.size() > 0 && innerPath.size() > 0) {
            createWhenInnerExist();
        } else if (outerPath.size() > 0) {
            // 只存在外部路径 == 新增递归对象主更新 + 新增一般对象从更新l
            createWhenInnerEmpty();
        } else {
            // 只存在内部路径 == 主更新splitTarget + 主更新普通字段
            createWhenOuterEmpty();
        }
    }

    public void createWhenInnerExist() {
        Query queryForSub = new Query();
        finalOids.forEach(id -> queryForSub.addCriteria(Criteria.where(Consts.OBJECT_ID).is(id)));
        createAtDoc(queryForSub, innerPath, lastPath, obj);
    }

    public void createWhenInnerEmpty() {
        Map<String, SplitQuery> recurFields = SplitSchemaConfig.recurFields;
        String arrName = lastPath.getPath();
        // 递归对象, 更新主即可
        if (recurFields.containsKey(arrName)) {
            obj = InitOpt.detachJson(obj, recurFields.get(arrName));
            Query queryForTree = new Query(Criteria.where(Consts.MONGO_KEY).is(mongoKey));
            createAtDoc(queryForTree, outerPath, lastPath, obj);
        } else {
            // 普通对象, 更新从即可
            Query queryForSub = new Query();
            finalOids.forEach(id -> queryForSub.addCriteria(Criteria.where(Consts.OBJECT_ID).is(id)));
            createAtDoc(queryForSub, new ArrayList<>(), lastPath, obj);
        }
    }

    private void createWhenOuterEmpty() {
        Map<String, SplitQuery> splitTarget = SplitSchemaConfig.splitTargets;
        String arrName = lastPath.getPath();
        if (splitTarget.containsKey(arrName)) {
            obj = InitOpt.detachJson(obj, splitTarget.get(arrName));
        }

        Query queryForTree = new Query(Criteria.where(Consts.MONGO_KEY).is(mongoKey));
        createAtDoc(queryForTree, innerPath, lastPath, obj);
    }


    /**
     * @description: find最终要create的文档，在数组指定位置添加对象，
     * @return:
     */
    private void createAtDoc(Query query, List<SegPath> paths, SegPath lastPath, Object obj) {
        Map map = (Map) obj;

        Update update = new Update();

        List<String> setKeys = MongoUtil.getSetKeys(paths, paths.size(), update);
        for (String setKey : setKeys) {
            String finalSetKey = setKey.isEmpty() ? "" : setKey + Consts.DOT;
            if (lastPath.getType().equals(SegPath.Type.field)) {
                // 最后一个path是field，特殊处理，默认append到数组最后一位
                finalSetKey += lastPath.getPath();
                update.push(finalSetKey, map);
            } else {
                // 最后一个path是数组和索引，不用解析到setKey
                finalSetKey += lastPath.getPath(); // 根据lastPath确定要create的数组
                int idx = ((IdxPath) lastPath).getIdx(); // // 根据lastPath确定要create的数组位置
                update.push(finalSetKey).atPosition(idx).value(map);
            }
        }
        UpdateResult app = getMongoTemplate().updateFirst(query, update, Consts.COLLECTION_NAME);
    }

}



























