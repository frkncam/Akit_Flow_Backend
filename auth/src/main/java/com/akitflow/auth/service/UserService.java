package com.akitflow.auth.service;

import com.akitflow.auth.dto.request.ChangePasswordRequest;
import com.akitflow.auth.dto.request.UpdateProfileRequest;
import com.akitflow.auth.dto.response.UserResponse;

public interface UserService {

    UserResponse getMyProfile(Long userId);

    UserResponse updateMyProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
