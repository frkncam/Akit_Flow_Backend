package com.akitflow.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Mail mail,
        String appBaseUrl,
        String signBaseUrl
) {
    public record Mail(String from, String fromName) {
    }
}
