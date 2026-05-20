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

import java.util.List;

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
                .orElseThrow(() -> new ResourceNotFoundException("Organizasyon bulunamadı."));
    }

    @Override
    @Transactional
    public OrganizationResponse updateMyOrganization(Long organizationId,
                                                     UpdateOrganizationRequest request) {
        var organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizasyon bulunamadı."));

        organization.setName(request.getName());
        organization.setSlug(SlugUtils.toSlug(request.getName()));

        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getMembers(Long organizationId) {
        return userRepository.findAllByOrganizationId(organizationId)
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeMember(Long organizationId, Long targetUserId) {
        var user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı."));

        if (!user.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("Kullanıcı bu organizasyona ait değil.");
        }

        refreshTokenRepository.revokeAllByUserId(targetUserId);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserResponse updateMemberRole(Long organizationId, Long targetUserId,
                                        UpdateMemberRoleRequest request) {
        var user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı."));

        if (!user.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("Kullanıcı bu organizasyona ait değil.");
        }

        user.setRole(request.getRole());
        return userMapper.toResponse(userRepository.save(user));
    }
}
