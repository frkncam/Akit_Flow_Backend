package com.akitflow.auth.dto.request;

import com.akitflow.auth.domain.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRoleRequest {

    @NotNull
    private UserRole role;
}
