package com.akitflow.signature.service;

import com.akitflow.common.client.dto.BatchSignatureRequest;
import com.akitflow.common.client.dto.SignatureDto;
import com.akitflow.signature.config.AppProperties;
import com.akitflow.signature.provider.ESignatureProvider;
import com.akitflow.signature.provider.ProviderSignatureResult;
import com.akitflow.signature.provider.SignerInfo;
import com.akitflow.signature.util.DocumentHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignatureRequestServiceImpl implements SignatureRequestService {

    private final ESignatureProvider provider;
    private final MinioService minioService;
    private final SignaturePersistenceService persistenceService;
    private final AppProperties appProperties;

    @Override
    public List<SignatureDto> sendForSignature(BatchSignatureRequest request, Long organizationId, Long actorId) {
        SignatureSetup setup = prepare(request, organizationId, actorId);
        ProviderSignatureResult providerRes = provider.requestSignatures(setup.toProviderRequest());
        return persistenceService.persist(setup, providerRes);
    }

    private SignatureSetup prepare(BatchSignatureRequest request, Long organizationId, Long actorId) {
        byte[] pdf = minioService.download(request.fileStorageKey());
        Duration validity = Duration.ofDays(appProperties.signature().token().validityDays());
        String documentHash = DocumentHasher.sha256Hex(pdf);

        List<SignerInfo> signers = request.signers().stream()
                .map(s -> new SignerInfo(s.name(), s.email()))
                .toList();

        return new SignatureSetup(request, organizationId, actorId, pdf, documentHash, signers, validity);
    }

    public record SignatureSetup(
            BatchSignatureRequest req,
            Long organizationId,
            Long actorId,
            byte[] pdf,
            String documentHash,
            List<SignerInfo> signers,
            Duration validity
    ) {
        com.akitflow.signature.provider.SignatureRequest toProviderRequest() {
            return new com.akitflow.signature.provider.SignatureRequest(
                    req.contractTitle(),
                    req.fileName(),
                    pdf,
                    signers,
                    validity
            );
        }
    }
}
