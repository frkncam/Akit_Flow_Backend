package com.akitflow.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String inviteBaseUrl,
        Invite invite
) {
    public record Invite(int tokenValidityHours) {}
}
