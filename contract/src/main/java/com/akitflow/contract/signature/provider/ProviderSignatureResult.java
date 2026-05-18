package com.akitflow.contract.signature.provider;

import java.util.Map;

public record ProviderSignatureResult(
        String providerName,
        /**
         * email → externalRef (provider'a göre signature/request id).
         * Stub provider için random UUID; gerçek provider'da gerçek ref.
         */
        Map<String, String> signerExternalRefs
) {}
