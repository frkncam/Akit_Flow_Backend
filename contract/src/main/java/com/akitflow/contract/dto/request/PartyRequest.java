package com.akitflow.contract.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PartyRequest(
        @NotBlank String name,
        @NotBlank String role,
        @Email String email
) {}
