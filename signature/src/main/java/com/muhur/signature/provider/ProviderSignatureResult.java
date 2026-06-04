package com.muhur.signature.provider;

import java.util.Map;

public record ProviderSignatureResult(
        String providerName,
        Map<String, String> signerExternalRefs
) {}
