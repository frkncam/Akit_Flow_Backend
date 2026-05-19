package com.akitflow.contract.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Minio minio
) {
    public record Minio(
            String url,
            String accessKey,
            String secretKey,
            String bucket,
            int presignedUrlExpiryMinutes
    ) {}
}
