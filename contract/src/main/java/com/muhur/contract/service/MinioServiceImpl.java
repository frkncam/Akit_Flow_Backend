package com.muhur.contract.service;

import com.muhur.contract.config.AppProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioServiceImpl implements MinioService {

    private final MinioClient client;
    private final AppProperties props;

    @PostConstruct
    void ensureBucket() {
        try {
            String bucket = props.minio().bucket();
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket created: {}", bucket);
            } else {
                log.info("MinIO bucket already exists: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO bucket bootstrap atlandı: {}", e.getMessage());
        }
    }

    @Override
    public void upload(String key, InputStream is, long size, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(props.minio().bucket())
                    .object(key)
                    .stream(is, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("MinIO upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String presignedGetUrl(String key) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(props.minio().bucket())
                    .object(key)
                    .expiry(props.minio().presignedUrlExpiryMinutes(), TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("MinIO pre-signed URL üretilemedi: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] download(String key) {
        try (InputStream stream = client.getObject(GetObjectArgs.builder()
                .bucket(props.minio().bucket())
                .object(key)
                .build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("MinIO download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.minio().bucket())
                    .object(key)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO obje silinemedi ({}): {}", key, e.getMessage());
        }
    }
}
