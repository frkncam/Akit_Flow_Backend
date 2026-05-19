package com.akitflow.auth.service;

import com.akitflow.auth.dto.request.ChangePasswordRequest;
import com.akitflow.auth.dto.request.UpdateProfileRequest;
import com.akitflow.auth.dto.response.UserResponse;
import com.akitflow.auth.exception.InvalidCredentialsException;
import com.akitflow.auth.exception.ResourceNotFoundException;
import com.akitflow.auth.mapper.UserMapper;
import com.akitflow.auth.repository.RefreshTokenRepository;
import com.akitflow.auth.repository.UserRepository;
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
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı."));
    }

    @Override
    @Transactional
    public UserResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı."));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Şifre değişince tüm aktif oturumları kapat
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
