package com.muhur.auth.controller;

import com.muhur.auth.dto.request.ChangePasswordRequest;
import com.muhur.auth.dto.request.UpdateProfileRequest;
import com.muhur.auth.dto.response.UserResponse;
import com.muhur.common.security.HeaderPrincipal;
import com.muhur.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse getMyProfile(@AuthenticationPrincipal HeaderPrincipal user) {
        return userService.getMyProfile(user.userId());
    }

    @PutMapping("/me")
    public UserResponse updateMyProfile(@Valid @RequestBody UpdateProfileRequest request,
                                        @AuthenticationPrincipal HeaderPrincipal user) {
        return userService.updateMyProfile(user.userId(), request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
                               @AuthenticationPrincipal HeaderPrincipal user) {
        userService.changePassword(user.userId(), request);
    }
}
