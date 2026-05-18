package com.akitflow.notification.event.payload;

public record UserJoinedPayload(
        String email,
        String firstName,
        String lastName,
        String organizationName,
        String role
) {
}
