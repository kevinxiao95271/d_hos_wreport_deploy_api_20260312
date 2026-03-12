package com.kxhospital.wreport.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wr.minio")
public class MinioProperties {

    private String endpoint    = "http://119.167.165.27:58010";
    private String accessKey   = "minioadmin";
    private String secretKey   = "Ygcx2025";
    /** 佐证材料桶 */
    private String bucketEvidence = "wk-registration-files";
    /** 格式模板桶 */
    private String bucketTemplate = "wk-system-templates";
    /** 预签名 URL 有效期（秒）*/
    private int    presignedExpire = 604800;
}
