package com.netease.cloud.lowcode.naslstorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
public class NaslstorageApplication {

	public static void main(String[] args) {
		SpringApplication.run(NaslstorageApplication.class, args);
	}

}
