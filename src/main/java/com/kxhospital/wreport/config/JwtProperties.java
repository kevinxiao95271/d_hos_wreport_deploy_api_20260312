package com.kxhospital.wreport.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wr.jwt")
public class JwtProperties {
    /** JWT 签名密钥（至少 32 字符）*/
    private String secret = "zjylzlHosWreportJwtSecret2025!@#$";
    /** Token 有效期（分钟），默认 8 小时 */
    private int expireMinutes = 480;
}
