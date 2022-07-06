package com.netease.cloud.lowcode.mongoHuge.operation;

import com.netease.cloud.lowcode.mongoHuge.common.Consts;
import com.netease.cloud.lowcode.mongoHuge.pathEntity.QueryPath;
import com.netease.cloud.lowcode.mongoHuge.splitSchema.SplitQuery;
import com.netease.cloud.lowcode.mongoHuge.splitSchema.SplitSchemaConfig;
import com.netease.cloud.lowcode.mongoHuge.util.MongoUtil;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/30 19:59
 */

@Component
public class InitOpt {

    public static void initHugeDoc(Object object) {
        // 处理每一条拆分逻辑
        for (SplitQuery splitQuery : SplitSchemaConfig.splitQueries) {
            // 定位到要拆分的位置
            QueryPath location = splitQuery.getLocation();
            // 需要改变结构的目标文档
            Map<String, Object> targetDoc = (Map) new QueryOpt().findExcludeLast(location, object);
            // 需要拆分出去的kv
            Object targetV = new QueryOpt().findForInit(object, location);
            String targetK = location.getLastPath().getPath();
            Object o = detachJson(targetV, splitQuery);
            targetDoc.put(targetK, o);
        }
        MongoUtil.insertDoc((Map) object);
    }

    /**
     * 分离Json，返回新的子结构
     */
    public static Object detachJson(Object json, SplitQuery splitQuery) {
        if (json instanceof List) {
            return detachArray(json, splitQuery);
        }
        if (json instanceof Map) {
            return detachObj(json, splitQuery);
        }
        return new Object();
    }

    /**
     * 拆分目标是数组，将数组分离，返回新的子结构
     */
    private static Object detachArray(Object targetV, SplitQuery splitQuery) {
        List<Map> arr = (List<Map>) targetV;
        if (arr.size() == 0) {
            return new ArrayList<>();
        }

        Map<String, Map> mark = new HashMap<>();
        List<Map> fakeSubStructures = new ArrayList<>();
        for (Map<String, Object> t : arr) {
            fakeSubStructures.add(getFakeRecur(mark, t, splitQuery));
        }
        // 延迟插入，同时还原fakeSubStructures真实结构
        delayInsertAndRestoreFake(mark, fakeSubStructures, splitQuery);
        //这里的fakeSubStructure已经恢复了
        return fakeSubStructures;
    }

    /**
     * @description: 拆分目标是数组，将对象分离，返回新的子结构
     * @return:
     */
    private static Object detachObj(Object targetV, SplitQuery splitQuery) {
        Map<String, Map> mark = new HashMap<>();
        Map fakeSubStructure = getFakeRecur(mark, (Map<String, Object>) targetV, splitQuery);

        // 延迟插入，同时还原fakeSubStructures真实结构
        delayInsertAndRestoreFake(mark, fakeSubStructure, splitQuery);

        //这里的fakeSubStructure已经恢复了
        return fakeSubStructure;
    }


    /**
     * @description: 延迟插入，获得Oid，同时将递归生成的假子结构恢复
     * @return:
     */
    private static void delayInsertAndRestoreFake(Map<String, Map> mark, Object fakeSubStructure, SplitQuery splitQuery) {
        // 先把mark中存的待插入的Map插入
        List<String> fakeOids = new ArrayList<>();
        List<Map> delayInsert = new ArrayList<>();
        for (Map.Entry<String, Map> entry : mark.entrySet()) {
            fakeOids.add(entry.getKey());
            delayInsert.add(entry.getValue());
        }
        // 批量存储，fakeOid和realOid映射
        long time = System.currentTimeMillis();
        List<ObjectId> realOids = MongoUtil.insertMany(delayInsert);
        System.out.println("INSERT MANY : " + (System.currentTimeMillis() - time));
        Map<String, ObjectId> fakeToReal = new HashMap<>();
        for (int i = 0; i < realOids.size(); i++) {
            fakeToReal.put(fakeOids.get(i), realOids.get(i));
        }

        // 递归恢复之前存的fakeOid
        if (fakeSubStructure instanceof Map) {
            healFakeRecur(fakeToReal, (Map<String, Object>) fakeSubStructure, splitQuery);
        }
        if (fakeSubStructure instanceof List) {
            List<Map> list = (List<Map>) fakeSubStructure;
            for (Map map : list) {
                healFakeRecur(fakeToReal, map, splitQuery);
            }
        }
    }


    /**
     * @description: 指定递归字段和连接字段，递归拆分当前对象
     * 注意：这里返回的子结构不是真实的子结构
     * 为了批量插入优化，此方法返回的Map里的所有oid是临时标记，插入完再恢复
     * @return: 单个引用链/多个引用链
     */
    private static Map getFakeRecur(Map<String, Map> mark, Map<String, Object> v, SplitQuery splitQuery) {
        String recurField = splitQuery.getRecurField();
        List<String> jointFields = splitQuery.getJointFields();

        Object sub = v.get(recurField);
        Object subStructure = new ArrayList<>();
        if (sub != null) {
            if (sub instanceof List) {
                List<Map> subStructures = new ArrayList<>();
                List<Map> subDocs = (List<Map>) sub;
                for (Map subDoc : subDocs) {
                    subStructures.add(getFakeRecur(mark, subDoc, splitQuery));
                }
                subStructure = subStructures;
            }
            if (sub instanceof Map) {
                subStructure = getFakeRecur(mark, (Map<String, Object>) sub, splitQuery);
            }
        }
        v.remove(recurField);

        // 假oid用于维持json结构
        String fakeOid = UUID.randomUUID().toString();

        // 记录假id对应的要延迟插入的文档
        mark.put(fakeOid, v);

        Map<String, Object> ret = new HashMap();
        ret.put(Consts.REFERENCE_OBJECT_ID, fakeOid);
        for (String jointField : jointFields) {
            ret.put(jointField, v.get(jointField));
        }
        if (recurField != null) {
            ret.put(splitQuery.recurField, subStructure);
        }
        return ret;
    }

    private static void healFakeRecur(Map<String, ObjectId> fakeToReal, Map<String, Object> v, SplitQuery splitQuery) {
        String recurField = splitQuery.getRecurField();

        Object sub = v.get(recurField);
        if (sub != null) {
            if (sub instanceof List) {
                List<Map> subDocs = (List<Map>) sub;
                for (Map subDoc : subDocs) {
                    healFakeRecur(fakeToReal, subDoc, splitQuery);
                }
            }
            if (sub instanceof Map) {
                healFakeRecur(fakeToReal, (Map<String, Object>) sub, splitQuery);
            }
        }

        if (v.containsKey(Consts.REFERENCE_OBJECT_ID)) {
            ObjectId readOid = fakeToReal.get(v.get(Consts.REFERENCE_OBJECT_ID));
            v.put(Consts.REFERENCE_OBJECT_ID, readOid);
        }
    }


    // -----------------------------------------------------------------------------------------------------

    /**
     * 删除JsonTree子结构
     */
    public static void deleteJson(Object json) {
        if (json instanceof List) {
            deleteArray(json);
        }
        if (json instanceof Map) {
            deleteObj(json);
        }
    }

    /**
     * 删除JsonTree数组
     */
    private static void deleteArray(Object targetV) {
        for (Map<String, Object> t : ((List<Map<String, Object>>) targetV)) {
            deleteObj(t);
        }
    }

    /**
     * 删除JsonTree对象
     */
    private static void deleteObj(Object targetV) {
        List<ObjectId> oids = new ArrayList<>();
        getOidRecur(oids, (Map<String, Object>) targetV);
        // 延迟批量删除
        MongoUtil.deleteMany(oids);
    }

    // 递归获得子结构所有oid
    private static void getOidRecur(List<ObjectId> mark, Map<String, Object> map) {
        mark.add((ObjectId) map.get(Consts.OBJECT_ID));
        for (String k : map.keySet()) {
            Object v = map.get(k);
            if (v != null) {
                if (v instanceof List) {
                    List<Object> arr = (List<Object>) v;
                    for (Object a : arr) {
                        if (a instanceof Map) {
                            getOidRecur(mark, (Map<String, Object>) a);
                        }
                    }
                }
                if (v instanceof Map) {
                    getOidRecur(mark, (Map<String, Object>) v);
                }
            }
        }
    }
}