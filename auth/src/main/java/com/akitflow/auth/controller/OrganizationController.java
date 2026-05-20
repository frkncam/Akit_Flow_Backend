package com.akitflow.auth.controller;

import com.akitflow.auth.dto.request.UpdateMemberRoleRequest;
import com.akitflow.auth.dto.request.UpdateOrganizationRequest;
import com.akitflow.auth.dto.response.OrganizationResponse;
import com.akitflow.auth.dto.response.UserResponse;
import com.akitflow.common.security.HeaderPrincipal;
import com.akitflow.auth.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping("/me")
    public OrganizationResponse getMyOrganization(@AuthenticationPrincipal HeaderPrincipal user) {
        return organizationService.getMyOrganization(user.organizationId());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('OWNER')")
    public OrganizationResponse updateMyOrganization(@Valid @RequestBody UpdateOrganizationRequest request,
                                                     @AuthenticationPrincipal HeaderPrincipal user) {
        return organizationService.updateMyOrganization(user.organizationId(), request);
    }

    @GetMapping("/me/members")
    public List<UserResponse> getMembers(@AuthenticationPrincipal HeaderPrincipal user) {
        return organizationService.getMembers(user.organizationId());
    }

    @DeleteMapping("/me/members/{userId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long userId,
                             @AuthenticationPrincipal HeaderPrincipal user) {
        organizationService.removeMember(user.organizationId(), userId);
    }

    @PutMapping("/me/members/{userId}/role")
    @PreAuthorize("hasRole('OWNER')")
    public UserResponse updateMemberRole(@PathVariable Long userId,
                                         @Valid @RequestBody UpdateMemberRoleRequest request,
                                         @AuthenticationPrincipal HeaderPrincipal user) {
        return organizationService.updateMemberRole(user.organizationId(), userId, request);
    }
}
