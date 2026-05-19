package com.akitflow.signature.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StubESignatureProvider implements ESignatureProvider {

    public static final String PROVIDER_NAME = "stub";

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public ProviderSignatureResult requestSignatures(SignatureRequest request) {
        Map<String, String> refs = request.signers().stream()
                .collect(Collectors.toMap(
                        SignerInfo::email,
                        s -> UUID.randomUUID().toString()
                ));
        log.info("[stub-signature] {} signer için imza isteği oluşturuldu (contract='{}')",
                refs.size(), request.contractTitle());
        return new ProviderSignatureResult(PROVIDER_NAME, refs);
    }

    @Override
    public Optional<byte[]> downloadSignedPdf(String externalRef) {
        return Optional.empty();
    }
}
