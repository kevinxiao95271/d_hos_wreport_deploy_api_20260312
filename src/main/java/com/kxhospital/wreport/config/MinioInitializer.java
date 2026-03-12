package com.kxhospital.wreport.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioInitializer implements ApplicationRunner {

    private final MinioService    minioService;
    private final MinioProperties props;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[MinIO] 初始化桶: {}, {}", props.getBucketEvidence(), props.getBucketTemplate());
        minioService.ensureBucket(props.getBucketEvidence());
        minioService.ensureBucket(props.getBucketTemplate());
    }
}
