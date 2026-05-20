package com.akitflow.auth.service;

import com.akitflow.auth.dto.request.UpdateMemberRoleRequest;
import com.akitflow.auth.dto.request.UpdateOrganizationRequest;
import com.akitflow.auth.dto.response.OrganizationResponse;
import com.akitflow.auth.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrganizationService {

    OrganizationResponse getMyOrganization(Long organizationId);

    OrganizationResponse updateMyOrganization(Long organizationId, UpdateOrganizationRequest request);

    Page<UserResponse> getMembers(Long organizationId, Pageable pageable);

    void removeMember(Long organizationId, Long targetUserId);

    UserResponse updateMemberRole(Long organizationId, Long targetUserId, UpdateMemberRoleRequest request);
}
