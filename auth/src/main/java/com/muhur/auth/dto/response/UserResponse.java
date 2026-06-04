package com.muhur.auth.dto.response;

import com.muhur.auth.domain.enums.UserRole;
import com.muhur.auth.domain.enums.UserStatus;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        UserStatus status,
        OrganizationResponse organization,
        Instant createdAt,
        Instant updatedAt
) {}
