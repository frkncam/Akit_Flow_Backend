package com.muhur.auth.service;

import com.muhur.auth.dto.request.ChangePasswordRequest;
import com.muhur.auth.dto.request.UpdateProfileRequest;
import com.muhur.auth.dto.response.UserResponse;
import com.muhur.auth.exception.InvalidCredentialsException;
import com.muhur.common.exception.ResourceNotFoundException;
import com.muhur.auth.mapper.UserMapper;
import com.muhur.auth.repository.RefreshTokenRepository;
import com.muhur.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    @Override
    @Transactional
    public UserResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Şifre değişince tüm aktif oturumları kapat
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
