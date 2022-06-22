package com.netease.cloud.lowcode.naslstorage.backend.mongo;

import com.netease.cloud.lowcode.naslstorage.backend.path.IdxPath;
import com.netease.cloud.lowcode.naslstorage.backend.path.KvPath;
import com.netease.cloud.lowcode.naslstorage.backend.path.SegmentPath;
import com.netease.cloud.lowcode.naslstorage.common.Consts;
import com.netease.cloud.lowcode.naslstorage.backend.PathConverter;
import lombok.SneakyThrows;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Service("mdbAppRepositoryImpl")
public class MdbQueryRepository {

    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private PathConverter<List<SegmentPath>> pathConverter;

    @SneakyThrows
    public Object get(RepositoryOperationContext context, String jsonPath, List<String> excludes) {
        List<SegmentPath> paths = pathConverter.convert(jsonPath);
        List<AggregationOperation> aggregationOperations = new ArrayList<>();
        /**
         * 文档过滤
         */
        if (!CollectionUtils.isEmpty(context.getObjectIds())) {
            aggregationOperations.add(Aggregation.match(Criteria.where(Consts.OBJECT_ID).in(context.getObjectIds())));
        }
        String lastPathWithoutParam = null;
        for (int i = 0; i < paths.size(); i++) {
            SegmentPath path = paths.get(i);
            if (path.getType() == SegmentPath.SegmentPathType.idx) {
                IdxPath tmp = (IdxPath) path;
                aggregationOperations.add(Aggregation.project().andExpression(tmp.getPath()).slice(1, Integer.valueOf(tmp.getIdx())));
                aggregationOperations.add(Aggregation.unwind(tmp.getPath()));
                aggregationOperations.add(Aggregation.replaceRoot(tmp.getPath()));
            } else if (path.getType() == SegmentPath.SegmentPathType.kv) {
                // key 和value 都不为空
                KvPath tmp = (KvPath) path;
                aggregationOperations.add(Aggregation.match(Criteria.where(tmp.getKey()).is(tmp.getValue())));
            } else {
                if (Consts.APP.equalsIgnoreCase(path.getPath())) {
                    // 应用过滤信息不在path 中，是通过header 传入的
                    aggregationOperations.add(Aggregation.match(Criteria.where(Consts.APP_ID).is(context.getAppId())));
                }
            }
            if (i < paths.size() - 1) {
                // 不是最后一个元素
                SegmentPath nextPath = paths.get(i + 1);
                // i+1 是最后一个元素并且没有搜索条件，则不进行replaceRoot
                if ((i + 1 == paths.size() -1) && SegmentPath.SegmentPathType.field == nextPath.getType()) {
                } else if (SegmentPath.SegmentPathType.kv == nextPath.getType()) {
                    // 非最后一段没有搜索条件的；非下一段是数组下标搜索
                    UnwindOperation unwindOperation = Aggregation.unwind(nextPath.getPath());
                    aggregationOperations.add(unwindOperation);
                    // mongo默认返回整个文档结构，我们需要返回查询的子node, 数组也不能作为newRoot
                    aggregationOperations.add(Aggregation.replaceRoot(nextPath.getPath()));
                }
            } else {
                // key、value 都为null，这种情况是路径上没有搜索条件, 通常是JsonObject 里选取字段。只有在最后一段path 中可能为数组，中间不带搜索条件的不能是数组。
                // mongodb 限制：非数组不能replaceRoot
                if (!Consts.APP.equalsIgnoreCase(path.getPath()) && SegmentPath.SegmentPathType.field == path.getType()) {
                    lastPathWithoutParam = path.getPath();
                }
            }
        }
        /**
         * 字段投影
         */
        if (!StringUtils.hasLength(lastPathWithoutParam) && !CollectionUtils.isEmpty(excludes)) {
            ProjectionOperation projectionOperation = new ProjectionOperation();
            for (String exclude : excludes) {
                projectionOperation = projectionOperation.andExclude(exclude);
            }
            aggregationOperations.add(projectionOperation);
        }

        Aggregation aggregation = Aggregation.newAggregation(aggregationOperations).withOptions(AggregationOptions.builder().allowDiskUse(true).build());

        AggregationResults<Map> aggregateResult = mongoTemplate.aggregate(aggregation, Consts.COLLECTION_NAME, Map.class);

        if (CollectionUtils.isEmpty(aggregateResult.getMappedResults())) {
            return new HashMap<>();
        }
        Map mongoResult = aggregateResult.getMappedResults().get(0);
        if (ObjectUtils.isEmpty(mongoResult)) {
            return mongoResult;
        }

        Object result = mongoResult;

        /**
         * 如果path最后一段没有参数，需要反射取字段，因为不能用replaceRoot
         */
        if (StringUtils.hasLength(lastPathWithoutParam)) {
            // 返回的可能是个数组或者object
            result = getProperty(mongoResult, lastPathWithoutParam, excludes);
        }

        return result;
    }

    private Object getProperty(Map doc, String lastPathWithoutParam, List<String> excludes) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (!StringUtils.hasLength(lastPathWithoutParam)) {
            return doc;
        }
        Method method = doc.getClass().getMethod("get", Object.class);
        Object ret = method.invoke(doc, lastPathWithoutParam);
        if (!CollectionUtils.isEmpty(excludes)) {
            for (String exclude : excludes) {
                if (ret instanceof Collection) {
                    Collection<Object> tmp = (Collection<Object>) ret;
                    tmp.forEach(v->{
                        if (v instanceof Map) {
                            ((Map<?, ?>) v).remove(exclude);
                        }
                    });
                    ret = tmp;
                } else if (ret instanceof Map) {
                    Map tmp = (Map) ret;
                    tmp.remove(exclude);
                    ret = tmp;
                }
            }
        }
        return ret;
    }
}
