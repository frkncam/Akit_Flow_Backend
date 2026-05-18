package com.akitflow.auth.event.payload;

public record UserRegisteredPayload(
        String email,
        String firstName,
        String lastName,
        String organizationName
) {
}
