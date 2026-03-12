package com.kxhospital.wreport;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.kxhospital.wreport.mapper")
@EnableConfigurationProperties
public class WrApplication {
    public static void main(String[] args) {
        SpringApplication.run(WrApplication.class, args);
    }
}
