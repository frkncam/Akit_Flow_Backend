package com.akitflow.notification.event.payload;

public record UserInvitedPayload(
        String email,
        String role,
        String inviteLink,
        String organizationName,
        String invitedByName
) {
}
