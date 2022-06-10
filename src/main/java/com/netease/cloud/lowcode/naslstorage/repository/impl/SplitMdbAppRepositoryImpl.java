package com.netease.cloud.lowcode.naslstorage.repository.impl;

import com.netease.cloud.lowcode.naslstorage.context.AppIdContext;
import com.netease.cloud.lowcode.naslstorage.context.RepositoryOperationContext;
import com.netease.cloud.lowcode.naslstorage.repository.AppRepository;
import com.netease.cloud.lowcode.naslstorage.service.JsonPathSchema;
import com.netease.cloud.lowcode.naslstorage.service.PathConverter;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Field;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.*;

/**
 * mdb 应用仓库包装类，提供文档拆分能力
 * @author pingerchen
 */
@Service("splitMdbAppRepositoryImpl")
public class SplitMdbAppRepositoryImpl implements AppRepository {
    @Resource(name = "mdbAppRepositoryImpl")
    private AppRepository appRepository;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private PathConverter<List<JsonPathSchema>> pathConverter;

    @Override
    public Object get(RepositoryOperationContext context, String jsonPath, List<String> excludes) {
        List<JsonPathSchema> pathSchemas = pathConverter.convert(jsonPath);
        // 路径上views.(children)+ 或logics在应用文档查询，其他的子path 需要定位到相应的关联文档操作
        // views.(children)+ 这段路径搜索只支持name或数组下标
        LocationDocument locationDocument = locationDoc(pathSchemas);
        return get(locationDocument.getNextJsonPath(), excludes, locationDocument.getObjectIds());
    }

    private Object get(String jsonPath, List<String> excludes, List<ObjectId> objectIds) {
        RepositoryOperationContext context = RepositoryOperationContext.builder().objectIds(objectIds).appId(AppIdContext.get()).build();
        Object ret = appRepository.get(context, jsonPath, excludes);
        return fillSubDoc(ret, excludes);
    }

    @Override
    public Map insert(String jsonPath, Map o) {
        return null;
    }

    @Override
    public void update(String jsonPath, Map o) {

    }

    private Object fillSubDoc(Object mongoResult, List<String> excludes) {
        /**
         * 文档组装
         */
        if (mongoResult instanceof Collection) {
            mongoResult = querySubDoc((List<Map>) mongoResult);
        } else {
            Map tmp = (Map) mongoResult;
            if (!ObjectUtils.isEmpty(tmp.get(REFERENCE_OBJECT_ID))) {
                // 本层有引用node，需要填充
                mongoResult = querySubDoc(tmp, excludes);
            } else {
                List<Map> viewsMap = (List<Map>) tmp.get(NEED_SPLIT_DOC_VIEWS);
                List<Map> childrenMap = (List<Map>) tmp.get(NEED_SPLIT_DOC_CHILDREN);
                List<Map> logicsMap = (List<Map>) tmp.get(NEED_SPLIT_DOC_LOGICS);
                if (!CollectionUtils.isEmpty(viewsMap)) {
                    tmp.put(NEED_SPLIT_DOC_VIEWS, querySubDoc(viewsMap));
                } else if (!CollectionUtils.isEmpty(childrenMap)) {
                    tmp.put(NEED_SPLIT_DOC_CHILDREN, querySubDoc(childrenMap));
                } else if (!CollectionUtils.isEmpty(logicsMap)) {
                    tmp.put(NEED_SPLIT_DOC_LOGICS, querySubDoc(logicsMap));
                }
            }
        }

        return mongoResult;
    }

    private List<Map> querySubDoc(List<Map> doc) {
        List<Map> ret = new ArrayList<>();
        if (CollectionUtils.isEmpty(doc)) {
            return ret;
        }
        List<ObjectId> objectIds = new ArrayList<>();
        Map<ObjectId, List<Map>> childrenMap = new HashMap<>();
        for (Map view : doc) {
            List<Map> children = (List<Map>) view.get(NEED_SPLIT_DOC_CHILDREN);
            List<Map> cret = new ArrayList<>();
            if (!CollectionUtils.isEmpty(children)) {
                cret = querySubDoc(children);
            }
            ObjectId objectId = (ObjectId) view.get(REFERENCE_OBJECT_ID);
            if (!ObjectUtils.isEmpty(objectId)) {
                childrenMap.put(objectId, cret);
                objectIds.add(objectId);
            } else {
                ret.add(view);
            }
        }
        if (!CollectionUtils.isEmpty(objectIds)) {
            Query query = Query.query(Criteria.where(OBJECT_ID).in(objectIds));
            List<Map> refRet = mongoTemplate.find(query, Map.class, COLLECTION_NAME);
            refRet.stream().forEach(v -> {
                if (!CollectionUtils.isEmpty(childrenMap.get(v.get(OBJECT_ID)))) {
                    v.put(NEED_SPLIT_DOC_CHILDREN, childrenMap.get(v.get(OBJECT_ID)));
                }
                ret.add(v);
            });
        }
        return ret;
    }

    private Map querySubDoc(Map c, List<String> excludes) {
        List<Map> children = (List<Map>) c.get(NEED_SPLIT_DOC_CHILDREN);
        List<Map> cret = new ArrayList<>();
        if (!CollectionUtils.isEmpty(children)) {
            cret = querySubDoc(children);
        }
        ObjectId objectId = (ObjectId) c.get(REFERENCE_OBJECT_ID);
        Query query = Query.query(Criteria.where(OBJECT_ID).is(objectId));
        Field field = query.fields();
        if (!CollectionUtils.isEmpty(excludes)) {
            for (String exclude : excludes) {
                field.exclude(exclude);
            }
        }
        Map ret = mongoTemplate.findOne(query, Map.class, COLLECTION_NAME);
        if (!CollectionUtils.isEmpty(children)) {
            ret.put(NEED_SPLIT_DOC_CHILDREN, cret);
        }
        return ret;
    }

    private LocationDocument locationDoc(List<JsonPathSchema> pathSchemas) {
        int lastIndex = -1;
        boolean needSplit = false;
        boolean notSplitProperty = true;
        boolean splitButInAppDoc = true;
        for (int i = 0; i < pathSchemas.size(); i++) {
            lastIndex = i;
            // views 下的logics 并不拆分文档
            notSplitProperty = i == 1 ? !NEED_SPLIT_FIRST_PROPERTY_IN_APP_DOC.contains(pathSchemas.get(i).getPath()) : notSplitProperty;
            splitButInAppDoc = i >= 2 ? pathSchemas.get(i).getPath().equalsIgnoreCase(NEED_SPLIT_DOC_CHILDREN) : splitButInAppDoc;
            if (!notSplitProperty && !splitButInAppDoc) {
                needSplit = true;
                break;
            }
        }
        LocationDocument locationDocument = new LocationDocument();
        if (needSplit) {
            List<ObjectId> objectIds = new ArrayList<>();
            Object appRet = appRepository.get(RepositoryOperationContext.builder().appId(AppIdContext.get()).build(), pathConverter.reverseConvert(pathSchemas.subList(0, lastIndex)), new ArrayList<>());
            if (appRet instanceof Collection) {
                ((Collection<?>) appRet).stream().forEach(v -> objectIds.add((ObjectId) ((Map) v).get(REFERENCE_OBJECT_ID)));
            } else {
                objectIds.add((ObjectId) ((Map) appRet).get(REFERENCE_OBJECT_ID));
            }
            locationDocument.setPreJsonPath(pathConverter.reverseConvert(pathSchemas.subList(0, lastIndex)));
            locationDocument.setNextJsonPath(pathConverter.reverseConvert(pathSchemas.subList(lastIndex, pathSchemas.size())));
            locationDocument.setObjectIds(objectIds);
        } else {
            // 不需要拆分
            locationDocument.setPreJsonPath(null);
            locationDocument.setNextJsonPath(pathConverter.reverseConvert(pathSchemas));
            locationDocument.setObjectIds(new ArrayList<>());
        }
        return locationDocument;
    }

    public class LocationDocument {
        private List<ObjectId> objectIds;
        private String nextJsonPath;
        private String preJsonPath;

        public List<ObjectId> getObjectIds() {
            return objectIds;
        }

        public String getNextJsonPath() {
            return nextJsonPath;
        }

        public void setNextJsonPath(String nextJsonPath) {
            this.nextJsonPath = nextJsonPath;
        }

        public void setObjectIds(List<ObjectId> objectIds) {
            this.objectIds = objectIds;
        }

        public String getPreJsonPath() {
            return preJsonPath;
        }

        public void setPreJsonPath(String preJsonPath) {
            this.preJsonPath = preJsonPath;
        }
    }
}
