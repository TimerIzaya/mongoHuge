package com.netease.cloud.lowcode.mongoHuge.operation;

import com.netease.cloud.lowcode.mongoHuge.common.Consts;
import com.netease.cloud.lowcode.mongoHuge.pathEntity.*;
import com.netease.cloud.lowcode.mongoHuge.pathEntity.QueryPath;
import com.netease.cloud.lowcode.mongoHuge.util.MongoUtil;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/29 14:43
 */

public class QueryOpt {

    // 用户侧使用的查找
    public static Object find(QueryPath queryPath) {
        String key = queryPath.getMongoKey();
        if (queryPath.getPath().size() == 0) {
            return completeQueryResult(MongoUtil.findDocByMongoKey(key), true);
        }
        Object obj = MongoUtil.findDocByMongoKey(key);
        List<SegPath> naslPath = queryPath.getPath();

        Object result = filterJsonByNaslPath(naslPath, obj);
        completeQueryResult(result, true);
        if (result instanceof List) {
            List list = (List) result;
            if (list.size() == 1) {
                return list.get(0);
            }
        }
        return result;
    }

    // 填充JsonTree 但不美化
    public static Object findWithOutBeautify(QueryPath queryPath) {
        String key = queryPath.getMongoKey();
        if (queryPath.getPath().size() == 0) {
            return completeQueryResult(MongoUtil.findDocByMongoKey(key), false);
        }
        Object obj = MongoUtil.findDocByMongoKey(key);
        List<SegPath> naslPath = queryPath.getPath();
        Object result = filterJsonByNaslPath(naslPath, obj);
        return completeQueryResult(result, false);
    }

    // 不填充JsonTree
    public static Object findWithOutComplete(QueryPath queryPath) {
        String key = queryPath.getMongoKey();
        if (queryPath.getPath().size() == 0) {
            return MongoUtil.findDocByMongoKey(key);
        }
        Object obj = MongoUtil.findDocByMongoKey(key);
        List<SegPath> naslPath = queryPath.getPath();
        return filterJsonByNaslPath(naslPath, obj);
    }


    // 直接在内存中对json进行查找, init操作使用
    public Object findForInit(Object obj, QueryPath queryPath) {
        List<SegPath> naslPath = queryPath.getPath();
        return filterJsonByNaslPath(naslPath, obj);
    }

    public Object findExcludeLast(QueryPath queryPath, Object obj) {
        List<SegPath> naslPath = queryPath.getPath();
        naslPath = naslPath.subList(0, naslPath.size() - 1);
        return filterJsonByNaslPath(naslPath, obj);
    }


    /**
     * @description: 用NaslPath查询整个Json
     * 用每一个SegPath过滤json
     */
    private static Object filterJsonByNaslPath(List<SegPath> naslPath, Object json) {
        for (SegPath segPath : naslPath) {
            json = json instanceof Map ? filterObjBySegPath(segPath, json) : filterArrBySegPath(segPath, json);
        }
        return json;
    }

    /**
     * @description: 用SegPath过滤Json中的对象
     * 如果过滤完的对象是Ref，则把它还原
     * 如果过滤完的对象是Ref数组，同上
     */
    public static Object filterObjBySegPath(SegPath segPath, Object obj) {
        SegPath.Type type = segPath.getType();
        if (type.equals(SegPath.Type.kv)) {
            obj = filterJsonByKvPath((KvPath) segPath, obj);
        } else if (type.equals(SegPath.Type.idx)) {
            obj = filterJsonByIdxPath((IdxPath) segPath, obj);
        } else if (type.equals(SegPath.Type.range)) {
            obj = filterJsonByRangePath((RangePath) segPath, obj);
        } else if (type.equals(SegPath.Type.field)) {
            obj = filterJsonByFieldPath((FieldPath) segPath, obj);
        }

        // todo 当前版本所有CURD全是走的关联字段，在JsonTree可以直接查，不用跳转
//        return jumpIfRefExist(obj);
        return obj;
    }

    /**
     * @description: 用SegPath过滤对象数组
     */
    public static Object filterArrBySegPath(SegPath segPath, Object obj) {
        List<Object> retList = new ArrayList<>();
        List<Object> objs = (List<Object>) obj;
        for (int i = 0; i < objs.size(); i++) {
            Object ret = filterObjBySegPath(segPath, objs.get(i));
            if (ret != null) {
                retList.add(ret);
            }
        }
        // 结果可能是多维的，需要压一下
        if (retList.size() > 0 && retList.get(0) instanceof List) {
            retList = (List<Object>) retList.stream().flatMap(list -> ((List) list).stream()).collect(Collectors.toList());
        }
        return retList;
    }


    /**
     * 用KvPath过滤Json
     * 用KvPath过滤Json中的数组
     * 用KvPath过滤Json中的对象
     */
    public static Object filterJsonByKvPath(KvPath kvPath, Object obj) {
        return obj instanceof Map ? filterObjByKvPath(kvPath, obj) : filterArrByKvPath(kvPath, obj);
    }

    public static Object filterArrByKvPath(KvPath kvPath, Object obj) {
        List<Object> objs = (List<Object>) obj;
        for (int i = 0; i < objs.size(); i++) {
            objs.set(i, filterObjByKvPath(kvPath, objs.get(i)));
        }
        return objs;
    }

    /**
     * @description:
     * @return: views[0].children[type=xxx]
     */
    public static Object filterObjByKvPath(KvPath kvPath, Object obj) {
        List<Object> retList = new ArrayList<>();

        // arr可能是引用数据
        List<Map<String, Object>> objArr = (List<Map<String, Object>>) ((Map) obj).get(kvPath.getPath());
        if (isRefObj(objArr)) {
            objArr = (List<Map<String, Object>>) jumpIfRefExist(objArr);
        }
        for (int i = 0; i < objArr.size(); i++) {
            Map<String, Object> e = objArr.get(i);
            if (e.get(kvPath.getKey()).equals(kvPath.getValue())) {
                retList.add(e);
                kvPath.getIdxs().add(i); //要同步记录选中元素在数组的位置
            }
        }
        return retList;
    }


    /**
     * 用KvPath过滤Json
     * 用KvPath过滤Json中的对象
     * 用KvPath过滤Json中的对象数组
     */
    public static Object filterJsonByIdxPath(IdxPath idxPath, Object obj) {
        return obj instanceof Map ? filterObjByIdxPath(idxPath, obj) : filterArrByIdxPath(idxPath, 0);
    }

    public static Object filterObjByIdxPath(IdxPath idxPath, Object obj) {
        List<Map<String, Object>> subMaps = (List<Map<String, Object>>) ((Map) obj).get(idxPath.getPath());
        int idx = idxPath.getIdx();
        if (idx >= 0 && idx < subMaps.size()) {
            return subMaps.get(idxPath.getIdx());
        }
        return null;
    }

    public static Object filterArrByIdxPath(IdxPath idxPath, Object obj) {
        List<Object> objs = (List<Object>) obj;
        for (int i = 0; i < objs.size(); i++) {
            objs.set(i, filterObjByIdxPath(idxPath, objs.get(i)));
        }
        return objs;
    }


    /**
     * 用RangePath过滤Json
     * 用RangePath过滤Json中的对象, 返回数组
     * 用RangePath过滤Json中的对象数组, 返回数组(根据Json语法，这里数组不内嵌，只有一维)
     */
    public static Object filterJsonByRangePath(RangePath rangePath, Object obj) {
        return obj instanceof Map ? filterObjByRangePath(rangePath, obj) : filterArrByRangePath(rangePath, obj);
    }

    public static Object filterObjByRangePath(RangePath rangePath, Object obj) {
        List<Map<String, Object>> subMaps = (List<Map<String, Object>>) ((Map) obj).get(rangePath.getPath());
        obj = subMaps.subList(rangePath.getStart(), rangePath.getEnd());
        return obj;
    }

    public static Object filterArrByRangePath(RangePath rangePath, Object obj) {
        List<Object> ret = new ArrayList<>();
        List<Object> objs = (List<Object>) obj;
        for (int i = 0; i < objs.size(); i++) {
            List<Object> list = (List<Object>) filterObjByRangePath(rangePath, obj);
            ret.addAll(list);
        }
        return ret;
    }


    /**
     * 用FieldPath过滤Json
     * 用FieldPath过滤Json中的对象
     * 用FieldPath过滤Json中的对象数组
     */
    public static Object filterJsonByFieldPath(FieldPath fieldPath, Object obj) {
        return obj instanceof Map ? filterObjByFieldPath(fieldPath, obj) : filterArrByFieldPath(fieldPath, obj);
    }

    public static Object filterObjByFieldPath(FieldPath fieldPath, Object obj) {
        obj = ((Map) obj).get(fieldPath.getValue());
        return obj;
    }

    public static Object filterArrByFieldPath(FieldPath fieldPath, Object obj) {
        List<Object> objs = (List<Object>) obj;
        for (int i = 0; i < objs.size(); i++) {
            objs.set(i, filterObjByFieldPath(fieldPath, objs.get(i)));
        }
        return objs;
    }


    /**
     * 如果obj是引用对象，则将它恢复
     * 1. 删除refId 2.保留recurField字段 3.添加其本体
     * obj是数组，依次递归调用每一个元素
     */
    public static Object jumpIfRefExist(Object obj) {
        if (obj instanceof Map) {
            if (isRefObj(obj)) {
                Map map = (Map) obj;
                ObjectId oid = (ObjectId) map.get(Consts.REFERENCE_OBJECT_ID);
                map.putAll(MongoUtil.findDocByOid(oid));
                map.remove(Consts.REFERENCE_OBJECT_ID);
                return map;
            }
        } else if (obj instanceof List) {
            List<Object> objs = (List<Object>) obj;
            for (int i = 0; i < objs.size(); i++) {
                objs.set(i, jumpIfRefExist(objs.get(i)));
            }
            return obj;
        }
        return obj;
    }

    /**
     * 判断一个Object是不是引用数据
     */
    public static boolean isRefObj(Object obj) {
        if (obj instanceof Map) {
            Map map = (Map) obj;
            return map.containsKey(Consts.REFERENCE_OBJECT_ID);
        } else if (obj instanceof List) {
            List list = (List) obj;
            for (Object e : list) {
                return isRefObj(e);
            }
        }
        return false;
    }

    /**
     * 将单个对象里的引用链填充为对应子文档
     */
    public static Object completeQueryResultObj(Object result, boolean beautify) {
        if (!(result instanceof List) && !(result instanceof Map)) {
            return result;
        }
        Map<String, Object> map = (Map) result;

        // result本身就是一个引用数据
        if (isRefObj(result)) {
            result = jumpIfRefExist(result);
            ((Map) result).remove(Consts.REFERENCE_OBJECT_ID);
            return completeQueryResultObj(result, beautify);
        }
        // 遍历每一个k，如果v是引用对象，则填充
        for (String k : map.keySet()) {
            Object v = map.get(k);
            if (v instanceof List) {
                List<Object> list = (List<Object>) v;
                v = list.stream().map(e -> {
                    if (isRefObj(e)) {
                        e = jumpIfRefExist(e);
                        ((Map) e).remove(Consts.REFERENCE_OBJECT_ID);
                        e = completeQueryResultObj(e, beautify);
                    }
                    return e;
                }).collect(Collectors.toList());
                map.put(k, v);
            } else if (v instanceof Map) {
                if (isRefObj(v)) {
                    v = jumpIfRefExist(v);
                    ((Map) v).remove(Consts.REFERENCE_OBJECT_ID);
                    v = completeQueryResultObj(v, beautify);
                    map.put(k, v);
                }
            }
        }

        // 去除一些对于查询无用的字段
        if (beautify) {
            map.remove(Consts.OBJECT_ID);
            map.remove(Consts.REFERENCE_OBJECT_ID);
            map.remove(Consts.MONGO_KEY);
        }
        return map;
    }

    public static Object completeQueryResultArr(Object result, boolean beautify) {
        return ((List) result).stream().map(e -> completeQueryResultObj(e, beautify)).collect(Collectors.toList());
    }

    private static Object completeQueryResult(Object result, boolean beautify) {
        if (result instanceof Map) {
            return completeQueryResultObj(result, beautify);
        } else if (result instanceof List) {
            return completeQueryResultArr(result, beautify);
        }
        return result;
    }
}





















