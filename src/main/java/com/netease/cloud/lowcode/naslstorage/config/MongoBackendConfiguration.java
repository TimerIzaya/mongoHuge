package com.netease.cloud.lowcode.naslstorage.config;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/22 11:34
 */

import com.netease.cloud.lowcode.naslstorage.backend.BackendStore;
import com.netease.cloud.lowcode.naslstorage.backend.mongo.MdbStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@ConditionalOnProperty(value = "backend.store.type", havingValue = "mongodb", matchIfMissing = true)
@EnableMongoRepositories
@EnableRetry
/**
 * 默认后端实现
 */
public class MongoBackendConfiguration {
    /**
     * 配置mongodb 事务
     * 增加此类 此外需要事务的方法上增加 @Transactional(rollbackFor = Exception.class) 开启事务
     */
    @Bean
    MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }

    /**
     * 必须
     * @return
     */
    @Bean
    BackendStore backendStore() {
        return new MdbStore();
    }
}