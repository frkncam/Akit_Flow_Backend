package com.muhur.auth.service;

import com.muhur.auth.dto.request.ChangePasswordRequest;
import com.muhur.auth.dto.request.UpdateProfileRequest;
import com.muhur.auth.dto.response.UserResponse;

public interface UserService {

    UserResponse getMyProfile(Long userId);

    UserResponse updateMyProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
