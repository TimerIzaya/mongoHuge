package com.netease.cloud.lowcode.naslstorage.backend.mongo;

import com.netease.cloud.lowcode.naslstorage.common.Consts;
import com.netease.cloud.lowcode.naslstorage.interceptor.AppIdContext;
import com.netease.cloud.lowcode.naslstorage.backend.JsonPathSchema;
import com.netease.cloud.lowcode.naslstorage.backend.PathConverter;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Field;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * mdb 应用仓库包装类，提供文档拆分能力
 * @author pingerchen
 */
@Service("splitMdbAppRepositoryImpl")
public class MdbSplitQueryRepositoryImpl {

    @Resource
    private MdbQueryRepositoryImpl mdbQueryRepository;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private PathConverter<List<JsonPathSchema>> pathConverter;

    public Object get(RepositoryOperationContext context, String jsonPath, List<String> excludes) {
        List<JsonPathSchema> pathSchemas = pathConverter.convert(jsonPath);
        // 路径上views.(children)+ 或logics在应用文档查询，其他的子path 需要定位到相应的关联文档操作
        // views.(children)+ 这段路径搜索只支持name或数组下标
        LocationDocument locationDocument = locationDoc(pathSchemas);
        return get(locationDocument, excludes);
    }

    private Object get(LocationDocument locationDocument, List<String> excludes) {
        Object ret;
        if (StringUtils.hasLength(locationDocument.getInnerJsonPath())) {
            RepositoryOperationContext context = RepositoryOperationContext.builder().objectIds(locationDocument.getObjectIds()).appId(AppIdContext.get()).build();
            ret = mdbQueryRepository.get(context, locationDocument.getInnerJsonPath(), excludes);
        } else {
            ret = mdbQueryRepository.get(RepositoryOperationContext.builder().appId(AppIdContext.get()).build(), locationDocument.getOutJsonPath(), new ArrayList<>());
        }
        return fillSubDoc(ret, excludes);
    }

    private Object fillSubDoc(Object mongoResult, List<String> excludes) {
        /**
         * 文档组装
         */
        if (mongoResult instanceof Collection) {
            mongoResult = querySubDoc((List<Map>) mongoResult);
        } else if (mongoResult instanceof Map) {
            Map tmp = (Map) mongoResult;
            if (!ObjectUtils.isEmpty(tmp.get(Consts.REFERENCE_OBJECT_ID))) {
                // 本层有引用node，需要填充
                mongoResult = querySubDoc(tmp, excludes);
            } else {
                List<Map> viewsMap = (List<Map>) tmp.get(Consts.NEED_SPLIT_DOC_VIEWS);
                List<Map> childrenMap = (List<Map>) tmp.get(Consts.NEED_SPLIT_DOC_CHILDREN);
                List<Map> logicsMap = (List<Map>) tmp.get(Consts.NEED_SPLIT_DOC_LOGICS);
                if (!CollectionUtils.isEmpty(viewsMap)) {
                    tmp.put(Consts.NEED_SPLIT_DOC_VIEWS, querySubDoc(viewsMap));
                }
                if (!CollectionUtils.isEmpty(logicsMap)) {
                    tmp.put(Consts.NEED_SPLIT_DOC_LOGICS, querySubDoc(logicsMap));
                }
                if (!CollectionUtils.isEmpty(childrenMap)) {
                    tmp.put(Consts.NEED_SPLIT_DOC_CHILDREN, querySubDoc(childrenMap));
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
            List<Map> children = (List<Map>) view.get(Consts.NEED_SPLIT_DOC_CHILDREN);
            List<Map> cret = new ArrayList<>();
            if (!CollectionUtils.isEmpty(children)) {
                cret = querySubDoc(children);
            }
            ObjectId objectId = (ObjectId) view.get(Consts.REFERENCE_OBJECT_ID);
            if (!ObjectUtils.isEmpty(objectId)) {
                childrenMap.put(objectId, cret);
                objectIds.add(objectId);
            } else {
                ret.add(view);
            }
        }
        if (!CollectionUtils.isEmpty(objectIds)) {
            Query query = Query.query(Criteria.where(Consts.OBJECT_ID).in(objectIds));
            List<Map> refRet = mongoTemplate.find(query, Map.class, Consts.COLLECTION_NAME);
            refRet.stream().forEach(v -> {
                if (!CollectionUtils.isEmpty(childrenMap.get(v.get(Consts.OBJECT_ID)))) {
                    v.put(Consts.NEED_SPLIT_DOC_CHILDREN, childrenMap.get(v.get(Consts.OBJECT_ID)));
                }
                ret.add(v);
            });
        }
        return ret;
    }

    private Map querySubDoc(Map c, List<String> excludes) {
        List<Map> children = (List<Map>) c.get(Consts.NEED_SPLIT_DOC_CHILDREN);
        List<Map> cret = new ArrayList<>();
        if (!CollectionUtils.isEmpty(children)) {
            cret = querySubDoc(children);
        }
        ObjectId objectId = (ObjectId) c.get(Consts.REFERENCE_OBJECT_ID);
        Query query = Query.query(Criteria.where(Consts.OBJECT_ID).is(objectId));
        Field field = query.fields();
        if (!CollectionUtils.isEmpty(excludes)) {
            for (String exclude : excludes) {
                field.exclude(exclude);
            }
        }
        Map ret = mongoTemplate.findOne(query, Map.class, Consts.COLLECTION_NAME);
        if (!CollectionUtils.isEmpty(children)) {
            ret.put(Consts.NEED_SPLIT_DOC_CHILDREN, cret);
        }
        return ret;
    }

    public LocationDocument locationDoc(List<JsonPathSchema> pathSchemas) {
        int lastIndex = -1;
        boolean needSplit = false;
        boolean notSplitProperty = true;
        boolean splitButInAppDoc = true;
        for (int i = 0; i < pathSchemas.size(); i++) {
            lastIndex = i;
            // views 下的logics 并不拆分文档
            notSplitProperty = i == 1 ? !Consts.NEED_SPLIT_FIRST_PROPERTY_IN_APP_DOC.contains(pathSchemas.get(i).getPath()) : notSplitProperty;
            splitButInAppDoc = i >= 2 ? pathSchemas.get(i).getPath().equalsIgnoreCase(Consts.NEED_SPLIT_DOC_CHILDREN) : splitButInAppDoc;
            if (!notSplitProperty && !splitButInAppDoc) {
                needSplit = true;
                break;
            }
        }
        LocationDocument locationDocument = new LocationDocument();
        List<ObjectId> objectIds = new ArrayList<>();
        List<JsonPathSchema> queryPath;
        List<JsonPathSchema> innerPath;
        if (needSplit) {
            queryPath = pathSchemas.subList(0, lastIndex);
            innerPath = pathSchemas.subList(lastIndex, pathSchemas.size());
        } else {
            // 不需要拆分
            queryPath = pathSchemas;
            innerPath = null;
        }
        Object appRet = mdbQueryRepository.get(RepositoryOperationContext.builder().appId(AppIdContext.get()).build(), pathConverter.reverseConvert(queryPath), new ArrayList<>());
        if (appRet instanceof Collection) {
            ((Collection<?>) appRet).stream().forEach(v -> objectIds.add((ObjectId) ((Map) v).get(Consts.REFERENCE_OBJECT_ID)));
        } else {
            objectIds.add((ObjectId) ((Map) appRet).get(Consts.REFERENCE_OBJECT_ID));
        }
        List<ObjectId> objectIdRet = objectIds.stream().filter(v->v!=null).collect(Collectors.toList());
        locationDocument.setOutJsonPath(pathConverter.reverseConvert(queryPath));
        locationDocument.setInnerJsonPath(pathConverter.reverseConvert(innerPath));
        locationDocument.setObjectIds(objectIdRet);
        return locationDocument;
    }


}
