package com.netease.cloud.lowcode.naslstorage.backend.mongo;

import com.mongodb.client.model.ReplaceOptions;
import com.netease.cloud.lowcode.naslstorage.common.Consts;
import com.netease.cloud.lowcode.naslstorage.dto.NaslChangedInfoDTO;
import com.netease.cloud.lowcode.naslstorage.enums.ChangedNaslType;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Repository
@Slf4j
public class MdbNaslChangedRecordRepository {
    private static final String APP_NASL_EXT_INFO_COLLECTION = "app_nasl_extend_info";
    private static final String APP_BACKEND_CHANGED_TIME = "backendChangedTime";
    private static final String APP_WEB_CHANGED_TIME = "webChangedTime";

    @Resource
    private MongoTemplate mongoTemplate;

    public void recordAppNaslChanged(String appId, ChangedNaslType naslType) {
        if (naslType == null || naslType == ChangedNaslType.none) {
            return;
        }
        Map<String, Object> record = new HashMap<>();
        Bson filter = Criteria.where(Consts.APP_ID).is(appId).getCriteriaObject();
        ReplaceOptions options = new ReplaceOptions();
        options = options.upsert(true);
        record.put(Consts.APP_ID, appId);
        switch (naslType) {
            case backend: {
                record.put(APP_BACKEND_CHANGED_TIME, System.currentTimeMillis());
                mongoTemplate.getCollection(APP_NASL_EXT_INFO_COLLECTION).replaceOne(filter, new Document(record), options);
                break;
            }
            case both: {
                record.put(APP_BACKEND_CHANGED_TIME, System.currentTimeMillis());
                record.put(APP_WEB_CHANGED_TIME, System.currentTimeMillis());
                mongoTemplate.getCollection(APP_NASL_EXT_INFO_COLLECTION).replaceOne(filter, new Document(record), options);
                break;
            }
            case web: {
                record.put(APP_WEB_CHANGED_TIME, System.currentTimeMillis());
                mongoTemplate.getCollection(APP_NASL_EXT_INFO_COLLECTION).replaceOne(filter, new Document(record), options);
                break;
            }
        }
    }

    public NaslChangedInfoDTO queryAppNaslChangedInfo(String appId) {
        Map<String, Object> ret = (Map<String, Object>) mongoTemplate.findOne(Query.query(Criteria.where(Consts.APP_ID).is(appId)), Map.class, APP_NASL_EXT_INFO_COLLECTION);
        NaslChangedInfoDTO dto = new NaslChangedInfoDTO();
        dto.setBackendChangedTime( ret == null || ret.get(APP_BACKEND_CHANGED_TIME) == null ? null : (Long) ret.get(APP_BACKEND_CHANGED_TIME));
        dto.setWebChangedTime(ret == null || ret.get(APP_WEB_CHANGED_TIME) == null ? null : (Long) ret.get(APP_WEB_CHANGED_TIME));
        return dto;
    }

}
