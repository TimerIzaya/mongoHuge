package com.netease.cloud.lowcode.naslstorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@ComponentScan(value = "com.netease.cloud.lowcode")
@SpringBootApplication
public class NaslstorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(NaslstorageApplication.class, args);
    }

}
