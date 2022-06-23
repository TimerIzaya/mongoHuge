package com.netease.cloud.lowcode.naslstorage.config;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/22 11:34
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;


/**
 * 配置mongodb 事务
 * 增加此类 此外需要事务的方法上增加 @Transactional(rollbackFor = Exception.class) 开启事务
 */
@Configuration
public class MongoTransactionConfiguration {

    @Bean
    MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }
}