package com.netease.cloud.lowcode.naslstorage.repository.impl;

import com.netease.cloud.lowcode.naslstorage.repository.AppRepository;
import com.netease.cloud.lowcode.naslstorage.service.PathConverter;
import javafx.util.Pair;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Field;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Service
public class MdbAppRepositoryImpl implements AppRepository {
    private static final String COLLECTION_NAME = "app";
    private static final String OBJECT_ID = "_id";

    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private PathConverter<List<Pair<String, String>>> pathConverter;

    @SneakyThrows
    @Override
    public Map get(String jsonPath, List<String> excludes) {
        List<Pair<String, String>> paths = pathConverter.convert(jsonPath);
        Query query = new Query();
        Criteria criteria = null;
        // 文档过滤
        Field field = query.fields().exclude("_id");
        Pair<String, String> includePair = null;
        for (Pair<String, String> path : paths) {
            if (path.getValue() == null) {
                break;
            }
            if (criteria == null) {
                criteria = Criteria.where(path.getKey()).is(path.getValue());
            } else {
                criteria.and(path.getKey()).is(path.getValue());
            }
            includePair = path;
        }
        /**
         * 字段投影
         * 默认只返回搜索路径上相关的node，路径上所有的pair 都取会Path collision，所以取最长的那个pair 即可
         */
        if (includePair != null) {
            String previewPath = pathConverter.getPreviousPath(includePair.getKey());
            // Include Embedded Fields in Array using slice
            // field.slice(previewPath, -1);
            // 排除掉除previewPath 外所有jsonNode 的返回
            /*if (StringUtils.hasLength(previewPath)) {
                field.include(previewPath);
            }*/
        }
        if (criteria != null) {
            query.addCriteria(criteria);
        }
        if (!CollectionUtils.isEmpty(excludes)) {
            for (String exclude : excludes) {
                field.exclude(exclude);
            }
        }
        Map result = mongoTemplate.findOne(query, Map.class, COLLECTION_NAME);
        if (ObjectUtils.isEmpty(result)) {
            return result;
        }
        // mongo默认返回整个文档结构，要返回子node，需要自己从mongo返回的文档中取
        result = getSubDoc(result, paths);
        if (!StringUtils.hasLength(includePair.getKey()) && (CollectionUtils.isEmpty(excludes) || !excludes.contains("views"))) {
            result.put("views", queryView((List<Map>) result.get("views")));
        } else {
            result.put("views", queryView((List<Map>) result.get("views")));
        }
        return result;
    }

    private Map getSubDoc(Map doc, List<Pair<String, String>> pairs) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (CollectionUtils.isEmpty(pairs)) {
            return doc;
        }
        Pair<String, String> pair = pairs.get(pairs.size()-1);
        String path = pathConverter.getPreviousPath(pair.getKey());
        if (!StringUtils.hasLength(path)) {
            return doc;
        }
        Map ret = doc;
        List<String> splitPaths = Arrays.asList(path.split("\\."));
        int i = 1;
        for (String splitPath : splitPaths) {
            Method method = ret.getClass().getMethod("get", Object.class);
            Object x = method.invoke(ret, splitPath);
            if (x instanceof ArrayList) {
                Object v = pairs.get(i).getValue();
                for (Map item : (ArrayList<Map>)x) {
                    if (Objects.equals(v, item.get("name"))) {
                        ret = item;
                    }
                }
            } else {
                ret = (Map)x;
            }
            i+=1;
        }
        return ret;
    }

    private List<Map> queryView(List<Map> c) {
        if (CollectionUtils.isEmpty(c)) {
            return new ArrayList<>();
        }
        List<ObjectId> objectIds = new ArrayList<>();
        Map<ObjectId, List<Map>> childrenMap = new HashMap<>();
        for (Map view : c) {
            List<Map> children = (List<Map>) view.get("children");
            List<Map> cret = new ArrayList<>();
            if (!CollectionUtils.isEmpty(children)) {
                cret = queryView(children);
            }
            ObjectId objectId = (ObjectId) view.get("refId");
            childrenMap.put(objectId, cret);
            objectIds.add(objectId);
        }
        Query query = Query.query(Criteria.where("_id").in(objectIds));
        List<Map> ret = mongoTemplate.find(query, Map.class, COLLECTION_NAME);
        ret.stream().forEach(v-> {
            v.put("children", childrenMap.get(v.get("_id")));
        });
        return ret;
    }

    private Map queryView(Map c) {
        List<Map> children = (List<Map>) c.get("children");
        List<Map> cret = new ArrayList<>();
        if (!CollectionUtils.isEmpty(children)) {
            cret = queryView(children);
        }
        ObjectId objectId = (ObjectId) c.get("refId");
        Query query = Query.query(Criteria.where("_id").is(objectId));
        Map ret = mongoTemplate.findOne(query, Map.class, COLLECTION_NAME);
        ret.put("children", cret);
        return ret;
    }

    @Override
    public Map insert(String jsonPath, Map o) {
        Document d = new Document(o);
        mongoTemplate.getCollection(COLLECTION_NAME).insertOne(d);
        o.put(OBJECT_ID, d.getObjectId(OBJECT_ID));
        return o;
    }

    @Override
    public void update(String jsonPath, Map o) {
        List<Pair<String, String>> paths = pathConverter.convert(jsonPath);
        Query query = new Query();
        Criteria criteria = null;
        // 文档过滤
        Field field = query.fields().exclude("_id");
        Pair<String, String> includePair = null;
        for (Pair<String, String> path : paths) {
            if (path.getValue() == null) {
                break;
            }
            if (criteria == null) {
                criteria = Criteria.where(path.getKey()).is(path.getValue());
            } else {
                criteria.and(path.getKey()).is(path.getValue());
            }
            includePair = path;
        }
        query.addCriteria(criteria);
        Update update = new Update();
        update.set("name", o.get("name"));
        mongoTemplate.updateFirst(query, update, COLLECTION_NAME);
    }

    // 取文档内某个object
        /*Query query = new Query(Criteria.where("playground.id").is("34b3d4462ff244e8975736d9bc5fa64f"));
        query.fields().exclude("playground.logicId");
        Map result = mongoTemplate.findOne(query, Map.class, COLLECTION_NAME);
        System.out.println(result.get("playground"));

        // 取文档内数组元素
        Query query1 = new Query(Criteria.where("body.id").is("b9fca871db9041568565f8fce3fb6d66"));
        query1.fields().elemMatch("body", Criteria.where("id").is("b9fca871db9041568565f8fce3fb6d66")).exclude("_id");
        Map result1 = mongoTemplate.findOne(query1, Map.class, COLLECTION_NAME);
        if (result1.get("body") instanceof ArrayList) {
            System.out.println(((ArrayList) result1.get("body")).get(0));
        }*/

    // 单个文档大于16M， 官方推荐用GridFS
    // mongoTemplate.save(body, COLLECTION_NAME);

    // 关联表，数组字段join，unwind 后，再group 合并困难
        /*Criteria criteria = Criteria.where("concept").is("App").and("name").is("course");
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.lookup("app", "_id", "preId", "view"),
                Aggregation.unwind("view", true),
                Aggregation.lookup("app", "view._id", "preId", "view.children")*//*,
                Aggregation.group("_id").getFields().and(Aggregation.group("_id").push("view").as("views").getFields().getField("xx"))*//*
        ).withOptions(AggregationOptions.builder().allowDiskUse(true).build());
        AggregationResults<Map> result = mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Map.class);
        System.out.println(result.getMappedResults());*/
}
