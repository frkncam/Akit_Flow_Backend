package com.akitflow.contract.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SendForSignatureRequest(
        @NotNull Long fileId,
        @Valid @NotEmpty List<SignerRequest> signers
) {}
