package com.muhur.auth.service;

import com.muhur.auth.dto.request.UpdateMemberRoleRequest;
import com.muhur.auth.dto.request.UpdateOrganizationRequest;
import com.muhur.auth.dto.response.OrganizationResponse;
import com.muhur.auth.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrganizationService {

    OrganizationResponse getMyOrganization(Long organizationId);

    OrganizationResponse updateMyOrganization(Long organizationId, UpdateOrganizationRequest request);

    Page<UserResponse> getMembers(Long organizationId, Pageable pageable);

    void removeMember(Long organizationId, Long targetUserId);

    UserResponse updateMemberRole(Long organizationId, Long targetUserId, UpdateMemberRoleRequest request);
}
