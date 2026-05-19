package com.akitflow.auth.service;

import com.akitflow.auth.dto.request.UpdateMemberRoleRequest;
import com.akitflow.auth.dto.request.UpdateOrganizationRequest;
import com.akitflow.auth.dto.response.OrganizationResponse;
import com.akitflow.auth.dto.response.UserResponse;

import java.util.List;

public interface OrganizationService {

    OrganizationResponse getMyOrganization(Long organizationId);

    OrganizationResponse updateMyOrganization(Long organizationId, UpdateOrganizationRequest request);

    List<UserResponse> getMembers(Long organizationId);

    void removeMember(Long organizationId, Long targetUserId);

    UserResponse updateMemberRole(Long organizationId, Long targetUserId, UpdateMemberRoleRequest request);
}
