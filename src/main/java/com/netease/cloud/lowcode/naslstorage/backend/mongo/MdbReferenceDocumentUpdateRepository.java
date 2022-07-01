package com.netease.cloud.lowcode.naslstorage.backend.mongo;

import com.netease.cloud.lowcode.naslstorage.common.Consts;
import com.netease.cloud.lowcode.naslstorage.backend.path.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.*;

/**
 * 1. 处理应用文档关联的 view 和 logic 文档
 * 2. 解析List<SegmentPath> paths获得setKey
 */
@Repository
public class MdbReferenceDocumentUpdateRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    public Map saveView(Map v) {
        List<Map> children = (List<Map>) v.get(Consts.CHILDREN);
        List<Map> childIds = new ArrayList<>();
        if (children != null) {
            for (Map child : children) {
                childIds.add(saveView(child));
            }
        }

        v.remove(Consts.CHILDREN);
        ObjectId objectId = insertDocument(v);

        Map<String, Object> retId = new HashMap();
        retId.put(Consts.REFERENCE_OBJECT_ID, objectId);
        retId.put(Consts.NAME, v.get(Consts.NAME));
        retId.put(Consts.CHILDREN, childIds);
        return retId;
    }

    public List<Map> saveViews(List<Map> viewList) {
        List<Map> retIds = new ArrayList<>();
        for (Map view : viewList) {
            retIds.add(saveView(view));
        }
        return retIds;
    }

    public Map saveLogic(Map logic) {
        ObjectId objectId = insertDocument(logic);
        Map newLogic = new HashMap();
        newLogic.put(Consts.REFERENCE_OBJECT_ID, objectId);
        newLogic.put("name", logic.get("name"));
        return newLogic;
    }

    public List<Map> saveLogics(List<Map> logicList) {
        List<Map> newLogicList = new ArrayList<>();
        logicList.forEach(logic -> {
            newLogicList.add(saveLogic(logic));
        });
        return newLogicList;
    }

    public void deleteView(Map v) {
        List<Map> children = (List<Map>) v.get(Consts.CHILDREN);
        if (children != null) {
            for (Map child : children) {
                deleteView(child);
            }
        }
        ObjectId id;
        if (v.containsKey(Consts.REFERENCE_OBJECT_ID)) {
            id = (ObjectId) v.get(Consts.REFERENCE_OBJECT_ID);
        } else {
            id = (ObjectId) v.get(Consts.OBJECT_ID);
        }
        removeDocument(id, Consts.APP);
    }

    public void deleteViews(List<Map> views) {
        for (Map view : views) {
            deleteView(view);
        }
    }

    public void deleteLogic(Map logic) {
        ObjectId objectId;
        if (logic.containsKey(Consts.REFERENCE_OBJECT_ID)) {
            objectId = (ObjectId) logic.get(Consts.REFERENCE_OBJECT_ID);
        } else {
            objectId = (ObjectId) logic.get(Consts.OBJECT_ID);
        }
        removeDocument(objectId, Consts.APP);
    }

    public void deleteLogics(List<Map> logics) {
        for (Map logic : logics) {
            deleteLogic(logic);
        }
    }

    public ObjectId insertDocument(Map o) {
        if (o.containsKey(Consts.OBJECT_ID)) {
            o.remove(Consts.OBJECT_ID);
        }
        Document d = new Document(o);
        mongoTemplate.getCollection(Consts.APP).insertOne(d);
        return d.getObjectId(Consts.OBJECT_ID);
    }

    public void removeDocument(ObjectId objectId, String collectionName) {
        Query query = new Query();
        query.addCriteria(Criteria.where(Consts.OBJECT_ID).is(objectId));
        mongoTemplate.remove(query, collectionName);
    }


    /**
     * 根据paths拼接setKey
     * 返回setKey列表，因为切片语法可能产生多个setKey
     * 注意！！！ 拼接setKey的同时给update添加了arrayFilter
     */
    public List<String> getSetKeys(List<SegmentPath> paths, int size, Update update) {
        if (paths.size() == 0) {
            List<String> setKeys = new ArrayList<>();
            setKeys.add("");
            return setKeys;
        }

        List<StringBuilder> setKeys = new ArrayList<>();
        setKeys.add(new StringBuilder());
        for (int i = 0; i < size; i++) {
            SegmentPath segmentPath = paths.get(i);
            if (segmentPath.getType() == SegmentPath.SegmentPathType.kv) {
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
            } else if (segmentPath.getType() == SegmentPath.SegmentPathType.idx) {
                IdxPath idxPath = (IdxPath) segmentPath;
                String arrName = idxPath.getPath();
                int idx = idxPath.getIdx();
                for (StringBuilder setKey : setKeys) {
                    setKey.append(arrName).append(".").append(idx);
                }
            } else if (segmentPath.getType() == SegmentPath.SegmentPathType.range) {
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

}
