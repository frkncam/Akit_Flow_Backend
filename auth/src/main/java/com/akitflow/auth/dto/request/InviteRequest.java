package com.akitflow.auth.dto.request;

import com.akitflow.auth.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteRequest {

    @NotBlank
    @Email
    private String email;

    @NotNull
    private UserRole role;
}
