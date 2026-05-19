package com.akitflow.signature.provider;

import java.util.Optional;

public interface ESignatureProvider {

    String providerName();

    ProviderSignatureResult requestSignatures(SignatureRequest request);

    Optional<byte[]> downloadSignedPdf(String externalRef);
}
