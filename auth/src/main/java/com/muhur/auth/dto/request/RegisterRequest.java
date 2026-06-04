package com.muhur.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Valid
        @NotNull
        UserRequest user,

        @NotBlank
        @Size(min = 8)
        String password
) {}
