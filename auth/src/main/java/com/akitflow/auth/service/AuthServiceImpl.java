package com.akitflow.auth.service;

import com.akitflow.auth.config.JwtProperties;
import com.akitflow.auth.domain.Organization;
import com.akitflow.auth.domain.RefreshToken;
import com.akitflow.auth.domain.User;
import com.akitflow.auth.domain.enums.UserRole;
import com.akitflow.auth.domain.enums.UserStatus;
import com.akitflow.auth.dto.request.LoginRequest;
import com.akitflow.auth.dto.request.OrganizationRequest;
import com.akitflow.auth.dto.request.RefreshTokenRequest;
import com.akitflow.auth.dto.request.RegisterRequest;
import com.akitflow.auth.dto.request.UserRequest;
import com.akitflow.auth.dto.response.AuthResponse;
import com.akitflow.auth.event.publisher.AuthEventPublisher;
import com.akitflow.auth.exception.EmailAlreadyExistsException;
import com.akitflow.auth.exception.InvalidCredentialsException;
import com.akitflow.auth.exception.InvalidTokenException;
import com.akitflow.auth.exception.OrganizationAlreadyExistsException;
import com.akitflow.auth.mapper.OrganizationMapper;
import com.akitflow.auth.mapper.UserMapper;
import com.akitflow.auth.repository.OrganizationRepository;
import com.akitflow.auth.repository.RefreshTokenRepository;
import com.akitflow.auth.repository.UserRepository;
import com.akitflow.auth.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;
    private final AuthEventPublisher eventPublisher;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        UserRequest userReq = request.user();
        OrganizationRequest orgReq = userReq.organization();

        if (userRepository.existsByEmail(userReq.email())) {
            throw new EmailAlreadyExistsException(userReq.email());
        }

        String slug = SlugUtils.toSlug(orgReq.name());
        if (organizationRepository.existsBySlug(slug)) {
            throw new OrganizationAlreadyExistsException(orgReq.name());
        }

        Organization organization = organizationMapper.toEntity(orgReq);
        organization.setSlug(slug);
        organization = organizationRepository.save(organization);

        User user = userMapper.toEntity(userReq);
        user.setOrganization(organization);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.OWNER);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        eventPublisher.publishUserRegistered(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = jwtService.hashToken(request.getRefreshToken());

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token geçersiz."));

        if (refreshToken.getRevokedAt() != null) {
            throw new InvalidTokenException("Refresh token iptal edilmiş.");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token süresi dolmuş.");
        }

        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);

        return buildAuthResponse(refreshToken.getUser());
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = jwtService.hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
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
