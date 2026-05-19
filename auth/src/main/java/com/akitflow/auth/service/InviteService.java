package com.akitflow.auth.service;

import com.akitflow.auth.dto.request.InviteAcceptRequest;
import com.akitflow.auth.dto.request.InviteRequest;
import com.akitflow.auth.dto.response.AuthResponse;

public interface InviteService {

    void invite(InviteRequest request, Long invitedById, Long organizationId);

    AuthResponse acceptInvite(InviteAcceptRequest request);
}
