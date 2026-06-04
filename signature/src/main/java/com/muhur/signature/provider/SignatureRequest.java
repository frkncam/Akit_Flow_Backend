package com.muhur.signature.provider;

import java.time.Duration;
import java.util.List;

public record SignatureRequest(
        String contractTitle,
        String fileName,
        byte[] pdfContent,
        List<SignerInfo> signers,
        Duration validity
) {}
