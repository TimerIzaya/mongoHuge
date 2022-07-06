package com.netease.cloud.lowcode.mongoHuge;

import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.model.IndexOptions;
import com.netease.cloud.lowcode.mongoHuge.common.Consts;
import com.netease.cloud.lowcode.mongoHuge.operation.*;
import com.netease.cloud.lowcode.mongoHuge.pathEntity.SegPath;
import com.netease.cloud.lowcode.mongoHuge.pathEntity.QueryPath;
import com.netease.cloud.lowcode.mongoHuge.util.MongoUtil;
import org.bson.Document;

import java.util.List;
import java.util.Map;


public class MongoHuge {


    public MongoHuge() {
        ListIndexesIterable<Document> indexList = MongoUtil.getMongoTemplate().getCollection(Consts.COLLECTION_NAME).listIndexes();
        //先检查是否存在索引
        for (Document document : indexList) {
            Object key = document.get("key");
            if (key instanceof Document) {
                Document keyDocument = (Document) key;
                if (keyDocument.containsKey(Consts.MONGO_KEY)) {
                    return;
                }
            }

        }

        //索引的属性配置
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.background(true);
        indexOptions.name(Consts.MONGO_KEY);

        MongoUtil.getMongoTemplate().getCollection(Consts.COLLECTION_NAME)
                // Document key为索引的列名称，value为索引类型
                .createIndex(new Document(Consts.MONGO_KEY, "hashed"), indexOptions);
        System.out.println("MongoKey Index Created");
    }

    public void jsonInit(String key, Object json) {
        // 说明当前Json已经存在，覆盖掉
        if (MongoUtil.findDocByMongoKey(key) != null) {
            jsonDel(key, new QueryPath());
        }
        Map map = (Map) json;
        map.put(Consts.MONGO_KEY, key);
        InitOpt.initHugeDoc(json);
    }

    public void jsonSet(String mongoKey, QueryPath queryPath, Object obj) {
        queryPath.setMongoKey(mongoKey);
        new UpdateOpt().update(queryPath, obj);
    }

    public Object jsonGet(String key, QueryPath queryPath) {
        queryPath.setMongoKey(key);
        return QueryOpt.find(queryPath);
    }

    public void jsonDel(String mongoKey, QueryPath queryPath) {
        queryPath.setMongoKey(mongoKey);
        List<SegPath> path = queryPath.getPath();
        // 如果没路径，说明删除整个文档
        if (path.size() == 0) {
            InitOpt.deleteJson(QueryOpt.findWithOutComplete(queryPath));
            return;
        }
        queryPath.setPath(path.subList(0, path.size() - 1));
        new DeleteOpt().delete(queryPath, path.get(path.size() - 1));
    }

    public void jsonArrInsert(String mongoKey, QueryPath queryPath, Object obj) {
        queryPath.setMongoKey(mongoKey);
        List<SegPath> path = queryPath.getPath();
        queryPath.setPath(path.subList(0, path.size() - 1));
        new CreateOpt().create(queryPath, path.get(path.size() - 1), obj);
    }


}
