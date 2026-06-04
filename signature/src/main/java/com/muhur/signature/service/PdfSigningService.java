package com.muhur.signature.service;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;

public interface PdfSigningService {

    SignResult sign(byte[] pdfContent, X509Certificate cert, PrivateKey key,
                    SignContext ctx) throws Exception;

    record SignContext(
            String signerName,
            String reason,
            String documentHash,
            String signerEmail,
            String signerIp,
            boolean otpVerified,
            String certificateSerial,
            String consentText,
            String algorithm
    ) {}

    record SignResult(
            byte[] pdf,
            Instant tsaTime,
            String tsaAuthority
    ) {}
}
