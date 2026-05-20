package com.akitflow.common.event.payload;

public record UserRegisteredPayload(
        String email,
        String firstName,
        String lastName,
        String organizationName
) {}
