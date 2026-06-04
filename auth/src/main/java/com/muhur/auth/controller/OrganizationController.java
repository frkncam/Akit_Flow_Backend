package com.muhur.auth.controller;

import com.muhur.auth.dto.request.UpdateMemberRoleRequest;
import com.muhur.auth.dto.request.UpdateOrganizationRequest;
import com.muhur.auth.dto.response.OrganizationResponse;
import com.muhur.auth.dto.response.UserResponse;
import com.muhur.common.security.HeaderPrincipal;
import com.muhur.auth.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public Page<UserResponse> getMembers(@AuthenticationPrincipal HeaderPrincipal user, Pageable pageable) {
        return organizationService.getMembers(user.organizationId(), pageable);
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
