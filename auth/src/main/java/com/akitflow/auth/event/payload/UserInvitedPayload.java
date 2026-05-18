package com.akitflow.auth.event.payload;

import com.akitflow.auth.domain.enums.UserRole;

public record UserInvitedPayload(
        String email,
        UserRole role,
        String inviteLink,
        String organizationName,
        String invitedByName
) {
}
