package com.akitflow.auth.event.payload;

import com.akitflow.auth.domain.enums.UserRole;

public record UserJoinedPayload(
        String email,
        String firstName,
        String lastName,
        String organizationName,
        UserRole role
) {
}
