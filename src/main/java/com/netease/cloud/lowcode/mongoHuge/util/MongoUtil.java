package com.netease.cloud.lowcode.mongoHuge.util;

/**
 * @description: 配置单例启动类
 * @author: sunhaoran
 * @time: 2022/6/30 17:21
 */

import com.mongodb.client.result.UpdateResult;
import com.netease.cloud.lowcode.mongoHuge.common.Consts;
import com.netease.cloud.lowcode.mongoHuge.pathEntity.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.*;

public class MongoUtil {

    public static MongoTemplate getMongoTemplate() {
        return (MongoTemplate) SpringUtil.getBean(MongoTemplate.class);
    }

    public static Map findDocByOid(ObjectId objectId) {
        return getMongoTemplate().findOne(new Query(Criteria.where(Consts.OBJECT_ID).is(objectId)), Map.class, Consts.COLLECTION_NAME);
    }

    public static Map findDocByMongoKey(String mongoKey) {
        return getMongoTemplate().findOne(new Query(Criteria.where(Consts.MONGO_KEY).is(mongoKey)), Map.class, Consts.COLLECTION_NAME);
    }

    /**
     * 插入单个文档，返回Oid
     */
    public static ObjectId insertDoc(Map o) {
        Document d = new Document(o);
        getMongoTemplate().getCollection(Consts.COLLECTION_NAME).insertOne(d);
        return d.getObjectId(Consts.OBJECT_ID);
    }

    /**
     * 插入多个文档，返回Oids
     */
    public static List<ObjectId> insertMany(List<Map> objs) {
        List<Document> docs = new ArrayList<>();
        objs.forEach(o -> docs.add(new Document(o)));
        getMongoTemplate().getCollection(Consts.COLLECTION_NAME).insertMany(docs);
        List<ObjectId> ret = new ArrayList<>();
        docs.forEach(d -> ret.add((ObjectId) d.get(Consts.OBJECT_ID)));
        return ret;
    }

    public static void deleteMany(List<ObjectId> oids) {
        Document d = new Document();
        d.put(Consts.OBJECT_ID, new Document("$in", oids));
        getMongoTemplate().getCollection(Consts.COLLECTION_NAME).deleteMany(d);
    }


    public static void dropCollection() {
        getMongoTemplate().getCollection(Consts.COLLECTION_NAME).drop();
    }

    /**
     * 根据paths拼接setKey
     * 返回setKey列表，因为切片语法可能产生多个setKey
     * 注意！！！ 拼接setKey的同时给update添加了arrayFilter
     */
    public static List<String> getSetKeys(List<SegPath> paths, int size, Update update) {
        if (paths.size() == 0) {
            List<String> setKeys = new ArrayList<>();
            setKeys.add("");
            return setKeys;
        }

        List<StringBuilder> setKeys = new ArrayList<>();
        setKeys.add(new StringBuilder());
        for (int i = 0; i < size; i++) {
            SegPath segmentPath = paths.get(i);
            if (segmentPath.getType() == SegPath.Type.kv) {
                KvPath kvPath = (KvPath) segmentPath;
                String arrName = kvPath.getPath();
                String key = kvPath.getKey();
                String value = kvPath.getValue();
                // setKey中的变量只能是数字和字母,且首位是字母
                String var = key + UUID.randomUUID().toString().replace("-", "");
                for (StringBuilder setKey : setKeys) {
                    setKey.append(arrName).append(".$[").append(var).append("]");
                }
                update.filterArray(Criteria.where(var + "." + key).is(value));
            } else if (segmentPath.getType() == SegPath.Type.idx) {
                IdxPath idxPath = (IdxPath) segmentPath;
                String arrName = idxPath.getPath();
                int idx = idxPath.getIdx();
                for (StringBuilder setKey : setKeys) {
                    setKey.append(arrName).append(".").append(idx);
                }
            } else if (segmentPath.getType() == SegPath.Type.range) {
                RangePath rangePath = (RangePath) segmentPath;
                int start = rangePath.getStart();
                int end = rangePath.getEnd();
                // 相当于给每个setKey都增加一次IdxPath
                List<StringBuilder> newSetKeys = new ArrayList<>();
                for (int idx = start; idx < end; idx++) {
                    for (StringBuilder setKey : setKeys) {
                        StringBuilder sb = new StringBuilder(setKey);
                        sb.append(rangePath.getPath()).append(".").append(idx);
                        newSetKeys.add(sb);
                    }
                }
                setKeys = newSetKeys;
            } else {
                FieldPath endPath = (FieldPath) segmentPath;
                String value = endPath.getValue();
                for (StringBuilder setKey : setKeys) {
                    setKey.append(value);
                }
            }
            for (StringBuilder setKey : setKeys) {
                setKey.append(".");
            }
        }

        for (StringBuilder setKey : setKeys) {
            if (setKey.length() > 0) {
                setKey.deleteCharAt(setKey.length() - 1);
            }
        }
        List<String> ret = new ArrayList<>();
        setKeys.forEach(s -> {
            ret.add(s.toString());
        });
        return ret;
    }

    public static void printUpdateResult(UpdateResult app) {
        System.out.println("match:" + app.getMatchedCount() + " modified:" + app.getModifiedCount());
    }

}