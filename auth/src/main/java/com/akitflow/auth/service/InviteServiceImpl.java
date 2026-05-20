package com.akitflow.auth.service;

import com.akitflow.auth.config.AppProperties;
import com.akitflow.auth.config.JwtProperties;
import com.akitflow.auth.domain.InviteToken;
import com.akitflow.auth.domain.RefreshToken;
import com.akitflow.auth.domain.User;
import com.akitflow.auth.domain.enums.UserStatus;
import com.akitflow.auth.dto.request.InviteAcceptRequest;
import com.akitflow.auth.dto.request.InviteRequest;
import com.akitflow.auth.dto.response.AuthResponse;
import com.akitflow.auth.event.publisher.AuthEventPublisher;
import com.akitflow.auth.exception.EmailAlreadyExistsException;
import com.akitflow.auth.exception.InviteTokenExpiredException;
import com.akitflow.auth.exception.InvalidTokenException;
import com.akitflow.common.exception.ResourceNotFoundException;
import com.akitflow.auth.mapper.UserMapper;
import com.akitflow.auth.repository.InviteTokenRepository;
import com.akitflow.auth.repository.OrganizationRepository;
import com.akitflow.auth.repository.RefreshTokenRepository;
import com.akitflow.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class InviteServiceImpl implements InviteService {

    private final InviteTokenRepository inviteTokenRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final AuthEventPublisher eventPublisher;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void invite(InviteRequest request, Long invitedById, Long organizationId) {
        // Idempotent: skip if active invite already exists
        if (inviteTokenRepository.existsByEmailAndOrganizationIdAndUsedAtIsNull(
                request.getEmail(), organizationId)) {
            return;
        }

        User inviter = userRepository.findById(invitedById)
                .orElseThrow(() -> new ResourceNotFoundException("Inviting user not found."));

        var organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found."));

        String rawToken = jwtService.generateRefreshToken();

        InviteToken savedInvite = inviteTokenRepository.save(
                InviteToken.builder()
                        .organization(organization)
                        .invitedBy(inviter)
                        .email(request.getEmail())
                        .role(request.getRole())
                        .tokenHash(jwtService.hashToken(rawToken))
                        .expiresAt(Instant.now().plusSeconds(appProperties.invite().tokenValidityHours() * 3600L))
                        .build()
        );

        eventPublisher.publishUserInvited(savedInvite, inviter, rawToken);
    }

    @Override
    @Transactional
    public AuthResponse acceptInvite(InviteAcceptRequest request) {
        String tokenHash = jwtService.hashToken(request.getToken());

        InviteToken inviteToken = inviteTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invite token is invalid."));

        if (inviteToken.getUsedAt() != null) {
            throw new InviteTokenExpiredException("Invite already used.");
        }

        if (inviteToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InviteTokenExpiredException("Invite has expired.");
        }

        if (userRepository.existsByEmail(inviteToken.getEmail())) {
            throw new EmailAlreadyExistsException(inviteToken.getEmail());
        }

        User user = userRepository.save(
                User.builder()
                        .organization(inviteToken.getOrganization())
                        .email(inviteToken.getEmail())
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .role(inviteToken.getRole())
                        .status(UserStatus.ACTIVE)
                        .build()
        );

        inviteToken.setUsedAt(Instant.now());
        inviteTokenRepository.save(inviteToken);

        eventPublisher.publishUserJoined(user);

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRefreshToken();

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .user(user)
                        .tokenHash(jwtService.hashToken(rawRefreshToken))
                        .expiresAt(Instant.now().plusSeconds(jwtProperties.refreshTokenExpiry()))
                        .build()
        );

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                jwtProperties.accessTokenExpiry(),
                userMapper.toResponse(user)
        );
    }
}
