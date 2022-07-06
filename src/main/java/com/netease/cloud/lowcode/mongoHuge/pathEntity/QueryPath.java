package com.netease.cloud.lowcode.mongoHuge.pathEntity;

import com.netease.cloud.lowcode.mongoHuge.common.Consts;
import com.netease.cloud.lowcode.mongoHuge.operation.QueryOpt;
import com.netease.cloud.lowcode.mongoHuge.pathSchema.PathConvertor;
import com.netease.cloud.lowcode.mongoHuge.pathSchema.PathType;
import com.netease.cloud.lowcode.mongoHuge.util.MongoUtil;
import lombok.Data;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/29 19:11
 */

@Data
public class QueryPath {

    // 默认查询方式为NaslPath
    private PathType type = PathType.NaslPath;

    // 初始路径
    private List<SegPath> path = new ArrayList<>();

    // 内部路径
    private List<SegPath> inner = new ArrayList<>();

    // 外部路径
    private List<SegPath> outer = new ArrayList<>();

    // 外部路径对应的子文档Oid
    private List<ObjectId> finalDocOids;

    // 要查询的json的jsonKey
    private String mongoKey;


    public QueryPath() {
    }

    public QueryPath(String rawPath) {
        this.path = PathConvertor.convertRawNaslPathToNaslPath(rawPath);
    }

    public QueryPath(String rawPath, PathType type) {
        this.type = type;
        if (type.equals(PathType.NaslPath)) {
            this.path = PathConvertor.convertRawNaslPathToNaslPath(rawPath);
        } else {
            this.path = PathConvertor.convertRawJsonPathToNaslPath(rawPath);
        }
    }

    public SegPath getLastPath() {
        return path.get(path.size() - 1);
    }

    public List<ObjectId> initOuterPath() {
        Object obj = MongoUtil.findDocByMongoKey(mongoKey);
        QueryOptForOuter queryOptForOuter = new QueryOptForOuter();
        return queryOptForOuter.find(obj, path);
    }


    /**
     * Update模块需要的find和Query的find略有不同，继承过来稍微修改一下
     * 1. 生成的OuterPath不能带有KvPath，因为更新主体json树的setKey，只能用索引定位到子结构
     * 2. 遍历segPath的过程中，如果发现某个seg过滤出的结果不包含Ref，那么这个seg就是outerPath的结尾
     * 3. 返回所有的最终文档Oid
     */
    private class QueryOptForOuter extends QueryOpt {

        public List<ObjectId> find(Object obj, List<SegPath> naslPath) {
            return filterJsonByNaslPath(naslPath, obj);
        }

        public List<ObjectId> filterJsonByNaslPath(List<SegPath> naslPath, Object json) {
            int outerSize = 0;
            for (int i = 0; i < naslPath.size(); i++) {
                SegPath seg = naslPath.get(i);
                // 如果当前过滤的json中，下一位segPath的arrName不是Ref了，说明outer结束
                if (isOuterEnd(json, seg.getPath())) {
                    break;
                }
                json = json instanceof Map ? filterObjBySegPath(seg, json) : filterArrBySegPath(seg, json);
                outerSize++;
            }
            json = jumpIfRefExist(json);

            outer = naslPath.subList(0, outerSize);
            inner = naslPath.subList(outerSize, naslPath.size());

            List<ObjectId> retList = new ArrayList<>();
            // 外部路径存在，说明需要跳转，返回所有需要跳转的Oid
            if (outer.size() > 0) {
                if (json instanceof Map) {
                    Map map = (Map) json;
                    retList.add((ObjectId) map.get(Consts.OBJECT_ID));
                } else if (json instanceof List) {
                    List<Map> list = (List<Map>) json;
                    for (Map map : list) {
                        retList.add((ObjectId) map.get(Consts.OBJECT_ID));
                    }
                }
            }
            return retList;
        }

        // 判断要进入的下一个v是不是引用数据，如果不是，则说明outerPath结束
        // 如果Json是数组，那么只要有一个对象下一个v是引用数据，则说明还没结束
        public boolean isOuterEnd(Object json, String nextField) {
            if (json instanceof List) {
                List list = (List) json;
                for (Object e : list) {
                    if (!isOuterEnd(e, nextField)) {
                        return false;
                    }
                }
                return true;
            } else if (json instanceof Map) {
                Map map = (Map) json;
                return !isRefObj(map.get(nextField));
            }
            // todo 当前json是字段(应该不会跑到这，待验证)
            return false;
        }
    }

    public boolean isEmpty() {
        return path.size() == 0;
    }

    public static void main(String[] args) {
        QueryPath queryPath = new QueryPath("views[0].children[1].name");
        queryPath.setMongoKey("app_0");
        List<ObjectId> objectIds = queryPath.initOuterPath();
        System.out.println("outer: " + queryPath.outer);
        System.out.println("inner: " + queryPath.inner);
        System.out.println(1);
    }
}

























