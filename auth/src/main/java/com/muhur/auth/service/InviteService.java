package com.muhur.auth.service;

import com.muhur.auth.dto.request.InviteAcceptRequest;
import com.muhur.auth.dto.request.InviteRequest;
import com.muhur.auth.dto.response.AuthResponse;

public interface InviteService {

    void invite(InviteRequest request, Long invitedById, Long organizationId);

    AuthResponse acceptInvite(InviteAcceptRequest request);
}
