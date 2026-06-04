package com.muhur.auth.dto.request;

import com.muhur.auth.domain.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRoleRequest {

    @NotNull
    private UserRole role;
}
