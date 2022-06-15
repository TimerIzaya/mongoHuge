package com.netease.cloud.lowcode.naslstorage.repository.impl;

import com.netease.cloud.lowcode.naslstorage.common.Global;
import com.netease.cloud.lowcode.naslstorage.entity.path.*;
import com.netease.cloud.lowcode.naslstorage.repository.RepositoryUtil;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.*;

@Repository
public class MdbRepositoryUtil implements RepositoryUtil {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public Map saveView(Map v) {
        List<Map> children = (List<Map>) v.get(Global.CHILDREN);
        List<Map> childIds = new ArrayList<>();
        if (children != null) {
            for (Map child : children) {
                childIds.add(saveView(child));
            }
        }
        v.put(Global.CHILDREN, childIds);
        ObjectId objectId = insertDocument(v);

        Map<String, Object> retId = new HashMap();
        retId.put(Global.REFERENCE_OBJECT_ID, objectId);
        retId.put("name", v.get("name"));
        retId.put(Global.CHILDREN, v.get(Global.CHILDREN));
        return retId;
    }

    @Override
    public List<Map> saveViews(List<Map> viewList) {
        List<Map> retIds = new ArrayList<>();
        for (Map view : viewList) {
            retIds.add(saveView(view));
        }
        return retIds;
    }

    @Override
    public Map saveLogic(Map logic) {
        ObjectId objectId = insertDocument(logic);
        Map newLogic = new HashMap();
        newLogic.put(Global.REFERENCE_OBJECT_ID, objectId);
        newLogic.put("name", logic.get("name"));
        return newLogic;
    }

    @Override
    public List<Map> saveLogics(List<Map> logicList) {
        List<Map> newLogicList = new ArrayList<>();
        logicList.forEach(logic -> {
            newLogicList.add(saveLogic(logic));
        });
        return newLogicList;
    }

    @Override
    public void deleteView(Map v) {
        List<Map> children = (List<Map>) v.get(Global.CHILDREN);
        if (children != null) {
            for (Map child : children) {
                deleteView(child);
            }
        }
        ObjectId id;
        if (v.containsKey(Global.REFERENCE_OBJECT_ID)) {
            id = (ObjectId) v.get(Global.REFERENCE_OBJECT_ID);
        } else {
            id = (ObjectId) v.get(Global.OBJECT_ID);
        }
        removeDocument(id, Global.APP);
    }

    @Override
    public void deleteViews(List<Map> views) {
        for (Map view : views) {
            deleteView(view);
        }
    }

    @Override
    public void deleteLogic(Map logic) {
        ObjectId objectId;
        if (logic.containsKey(Global.REFERENCE_OBJECT_ID)) {
            objectId = (ObjectId) logic.get(Global.REFERENCE_OBJECT_ID);
        } else {
            objectId = (ObjectId) logic.get(Global.OBJECT_ID);
        }
        removeDocument(objectId, Global.APP);
    }

    @Override
    public void deleteLogics(List<Map> logics) {
        for (Map logic : logics) {
            deleteLogic(logic);
        }
    }

    @Override
    public ObjectId insertDocument(Map o) {
        Document d = new Document(o);
        mongoTemplate.getCollection(Global.APP).insertOne(d);
        return d.getObjectId(Global.OBJECT_ID);
    }

    @Override
    public void removeDocument(ObjectId objectId, String collectionName) {
        Query query = new Query();
        query.addCriteria(Criteria.where(Global.OBJECT_ID).is(objectId));
        mongoTemplate.remove(query, collectionName);
    }


    /**
     * 根据paths拼接setKey
     * 返回setKey列表，因为切片语法可能产生多个setKey
     * 注意！！！ 拼接setKey的同时给update添加了arrayFilter
     */
    public List<String> getSetKeys(List<PartPath> paths, int size, Update update) {
        if (paths.size() == 0) {
            List<String> setKeys = new ArrayList<>();
            setKeys.add("");
            return setKeys;
        }

        List<StringBuilder> setKeys = new ArrayList<>();
        setKeys.add(new StringBuilder());
        for (int i = 0; i < size; i++) {
            PartPath partPath = paths.get(i);
            if (partPath.getType().equals(Global.PATH_TYPE_KV)) {
                KvPath kvPath = (KvPath) partPath;
                String arrName = kvPath.getArrName();
                String key = kvPath.getKey();
                String value = kvPath.getValue();
                // setKey中的变量只能是数字和字母,且首位是字母
                String var = key + UUID.randomUUID().toString().replace("-", "");
                for (StringBuilder setKey : setKeys) {
                    setKey.append(arrName).append(".$[").append(var).append("]");
                }
                update.filterArray(Criteria.where(var + "." + key).is(value));
            } else if (partPath.getType().equals(Global.PATH_TYPE_IDX)) {
                IdxPath idxPath = (IdxPath) partPath;
                String arrName = idxPath.getArrName();
                int idx = idxPath.getIdx();
                for (StringBuilder setKey : setKeys) {
                    setKey.append(arrName).append(".").append(idx);
                }
            } else if (partPath.getType().equals(Global.PATH_TYPE_RANGE)) {
                RangePath rangePath = (RangePath) partPath;
                int start = rangePath.getStart();
                int end = rangePath.getEnd();
                // 相当于给每个setKey都增加一次IdxPath
                List<StringBuilder> newSetKeys = new ArrayList<>();
                for (int idx = start; idx < end; idx++) {
                    for (StringBuilder setKey : setKeys) {
                        StringBuilder sb = new StringBuilder(setKey);
                        sb.append(rangePath.getArrName()).append(".").append(idx);
                        newSetKeys.add(sb);
                    }
                }
                setKeys = newSetKeys;
            } else {
                FieldPath endPath = (FieldPath) partPath;
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
