package com.akitflow.signature.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Minio minio,
        Signature signature
) {
    public record Minio(
            String url,
            String accessKey,
            String secretKey,
            String bucket,
            int presignedUrlExpiryMinutes
    ) {}

    public record Signature(
            Certificate certificate,
            Appearance appearance,
            Token token
    ) {
        public record Certificate(
                int validityYears,
                int keySize,
                String algorithm
        ) {}

        public record Appearance(
                int margin,
                int boxWidth,
                int boxHeight,
                int fontSize,
                String timezone,
                String labelTr,
                String labelEn
        ) {}

        public record Token(
                int validityDays
        ) {}
    }
}
