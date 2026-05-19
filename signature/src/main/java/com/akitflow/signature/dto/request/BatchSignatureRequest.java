package com.akitflow.signature.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchSignatureRequest(
        @NotNull Long contractId,
        @NotBlank @Size(max = 255) String contractTitle,
        @NotNull Long fileId,
        @NotBlank String fileStorageKey,
        @NotBlank @Size(max = 255) String fileName,
        @NotNull Long organizationId,
        @Valid @NotEmpty List<SignerRequest> signers
) {}
