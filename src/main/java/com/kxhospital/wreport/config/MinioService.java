package com.kxhospital.wreport.config;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient    minioClient;
    private final MinioProperties props;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ========= 上传 =========

    public String uploadEvidence(MultipartFile file, String prefix) {
        return upload(file, props.getBucketEvidence(), prefix);
    }

    public String uploadTemplate(MultipartFile file, String prefix) {
        return upload(file, props.getBucketTemplate(), prefix);
    }

    public String upload(MultipartFile file, String bucket, String prefix) {
        String objectName = buildObjectName(file.getOriginalFilename(), prefix);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            String url = directUrl(bucket, objectName);
            log.info("[MinIO] uploaded bucket={} object={}", bucket, objectName);
            return url;
        } catch (Exception e) {
            log.error("[MinIO] upload failed: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    // ========= 下载 =========

    /**
     * 从 MinIO 获取对象输入流，由调用方负责关闭。
     * url 格式：{endpoint}/{bucket}/{objectName}
     */
    public java.io.InputStream getObjectStream(String bucket, String url) {
        try {
            String objectName = extractObjectName(url, bucket);
            return minioClient.getObject(
                    io.minio.GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("获取文件流失败: " + e.getMessage(), e);
        }
    }

    // ========= 删除 =========

    public void deleteByUrl(String bucket, String url) {
        if (url == null || url.isEmpty()) return;
        try {
            String objectName = extractObjectName(url, bucket);
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
        } catch (Exception e) {
            log.warn("[MinIO] delete failed (ignored): {}", e.getMessage());
        }
    }

    // ========= 预签名 =========

    public String presignedUrl(String bucket, String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET).bucket(bucket).object(objectName)
                    .expiry(props.getPresignedExpire(), TimeUnit.SECONDS).build());
        } catch (Exception e) {
            throw new RuntimeException("获取文件链接失败: " + e.getMessage(), e);
        }
    }

    // ========= 初始化桶 =========

    public void ensureBucket(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                String policy = String.format(
                        "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\"," +
                        "\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"]," +
                        "\"Resource\":[\"arn:aws:s3:::%s/*\"]}]}", bucket);
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
                log.info("[MinIO] bucket created & policy set: {}", bucket);
            }
        } catch (Exception e) {
            log.error("[MinIO] init bucket {} failed: {}", bucket, e.getMessage(), e);
        }
    }

    // ========= helpers =========

    private String buildObjectName(String originalName, String prefix) {
        String ext  = FilenameUtils.getExtension(originalName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String date = LocalDateTime.now().format(DATE_FMT);
        String name = uuid + (ext != null && !ext.isEmpty() ? "." + ext : "");
        return (prefix != null && !prefix.isEmpty()) ? prefix + "/" + date + "/" + name : date + "/" + name;
    }

    public String directUrl(String bucket, String objectName) {
        String ep = props.getEndpoint();
        if (ep.endsWith("/")) ep = ep.substring(0, ep.length() - 1);
        return ep + "/" + bucket + "/" + objectName;
    }

    public String extractObjectName(String url, String bucket) {
        String marker = "/" + bucket + "/";
        int idx = url.indexOf(marker);
        if (idx == -1) throw new RuntimeException("Cannot extract objectName from URL: " + url);
        return url.substring(idx + marker.length());
    }
}
