package com.netease.cloud.lowcode.naslstorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;

@SpringBootApplication
@EnableMongoRepositories
@EnableRetry
public class NaslstorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(NaslstorageApplication.class, args);
    }

}
