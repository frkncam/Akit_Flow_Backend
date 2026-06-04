package com.muhur.signature.dto.response;

import com.muhur.signature.domain.enums.SignatureStatus;

import java.time.Instant;

public record SignatureViewResponse(
        String contractTitle,
        String signerName,
        String signerEmail,
        String pdfUrl,
        String signedPdfUrl,
        Instant expiresAt,
        SignatureStatus status,
        String signatureMetadata,
        boolean otpRequired,
        boolean otpVerified
) {}
