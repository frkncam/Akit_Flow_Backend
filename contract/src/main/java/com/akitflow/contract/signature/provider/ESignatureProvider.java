package com.akitflow.contract.signature.provider;

import java.util.Optional;

/**
 * Abstraction over e-signature providers (stub for dev, ImzaCepte/KEP/NES
 * for production). Phase 3A ships only the stub; Phase 3B drops in a real
 * provider without touching the rest of the workflow.
 */
public interface ESignatureProvider {

    /**
     * Provider identifier persisted on each signature row.
     */
    String providerName();

    /**
     * Initiates signature requests for all signers and returns the
     * provider-side reference for each (keyed by signer email).
     */
    ProviderSignatureResult requestSignatures(SignatureRequest request);

    /**
     * For providers that return a signed PDF after all parties sign.
     * Stub provider returns Optional.empty() since it does not embed
     * cryptographic signatures.
     */
    Optional<byte[]> downloadSignedPdf(String externalRef);
}
