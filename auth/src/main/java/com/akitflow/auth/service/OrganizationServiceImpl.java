package com.akitflow.auth.service;

import com.akitflow.auth.dto.request.UpdateMemberRoleRequest;
import com.akitflow.auth.dto.request.UpdateOrganizationRequest;
import com.akitflow.auth.dto.response.OrganizationResponse;
import com.akitflow.auth.dto.response.UserResponse;
import com.akitflow.common.exception.ResourceNotFoundException;
import com.akitflow.auth.mapper.OrganizationMapper;
import com.akitflow.auth.mapper.UserMapper;
import com.akitflow.auth.repository.OrganizationRepository;
import com.akitflow.auth.repository.RefreshTokenRepository;
import com.akitflow.auth.repository.UserRepository;
import com.akitflow.auth.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OrganizationMapper organizationMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getMyOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .map(organizationMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found."));
    }

    @Override
    @Transactional
    public OrganizationResponse updateMyOrganization(Long organizationId,
                                                     UpdateOrganizationRequest request) {
        var organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found."));

        organization.setName(request.getName());
        organization.setSlug(SlugUtils.toSlug(request.getName()));

        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getMembers(Long organizationId, Pageable pageable) {
        return userRepository.findAllByOrganizationId(organizationId, pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public void removeMember(Long organizationId, Long targetUserId) {
        var user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!user.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("User does not belong to this organization.");
        }

        refreshTokenRepository.revokeAllByUserId(targetUserId);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserResponse updateMemberRole(Long organizationId, Long targetUserId,
                                        UpdateMemberRoleRequest request) {
        var user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!user.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("User does not belong to this organization.");
        }

        user.setRole(request.getRole());
        return userMapper.toResponse(userRepository.save(user));
    }
}
