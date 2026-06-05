package com.muhur.contract.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Minio minio,
        Scheduler scheduler
) {
    public record Minio(
            String url,
            String accessKey,
            String secretKey,
            String bucket,
            int presignedUrlExpiryMinutes,
            String publicUrl
    ) {}

    public record Scheduler(
            String contractExpiringCron,
            String contractExpiringZone
    ) {}
}
