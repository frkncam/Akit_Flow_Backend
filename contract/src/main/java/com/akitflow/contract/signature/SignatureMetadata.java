package com.akitflow.contract.signature;

import java.time.Instant;

public record SignatureMetadata(
        Instant signedAt,
        String signerName,
        String signerEmail,
        String algorithm,
        String certificateSerial
) {}
