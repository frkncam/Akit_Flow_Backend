package com.akitflow.signature.service;

import com.akitflow.signature.config.AppProperties;
import com.akitflow.signature.domain.Signature;
import com.akitflow.signature.domain.enums.SignatureStatus;
import com.akitflow.signature.dto.request.BatchSignatureRequest;
import com.akitflow.signature.dto.response.SignatureResponse;
import com.akitflow.signature.event.payload.SignatureRequestedPayload;
import com.akitflow.signature.event.publisher.SignatureEventPublisher;
import com.akitflow.signature.mapper.SignatureMapper;
import com.akitflow.signature.provider.ESignatureProvider;
import com.akitflow.signature.provider.ProviderSignatureResult;
import com.akitflow.signature.provider.SignerInfo;
import com.akitflow.signature.repository.SignatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignatureRequestServiceImpl implements SignatureRequestService {

    private final SignatureRepository signatureRepository;
    private final SignatureMapper mapper;
    private final ESignatureProvider provider;
    private final MinioService minioService;
    private final SignatureEventPublisher eventPublisher;
    private final AppProperties appProperties;

    @Override
    public List<SignatureResponse> sendForSignature(BatchSignatureRequest request, Long organizationId) {
        SignatureSetup setup = prepare(request, organizationId);
        ProviderSignatureResult providerRes = provider.requestSignatures(setup.toProviderRequest());
        return persist(setup, providerRes);
    }

    private SignatureSetup prepare(BatchSignatureRequest request, Long organizationId) {
        byte[] pdf = minioService.download(request.fileStorageKey());
        Duration validity = Duration.ofDays(appProperties.signature().token().validityDays());

        List<SignerInfo> signers = request.signers().stream()
                .map(s -> new SignerInfo(s.name(), s.email()))
                .toList();

        return new SignatureSetup(request, organizationId, pdf, signers, validity);
    }

    private record SignatureSetup(
            BatchSignatureRequest req,
            Long organizationId,
            byte[] pdf,
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

    @Transactional
    List<SignatureResponse> persist(SignatureSetup setup, ProviderSignatureResult providerRes) {
        Long contractId = setup.req().contractId();
        Long orgId = setup.organizationId();

        signatureRepository.findAllByContractIdAndStatus(contractId, SignatureStatus.PENDING)
                .forEach(s -> s.setStatus(SignatureStatus.CANCELLED));

        UUID batchId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(setup.validity());

        List<Signature> created = setup.signers().stream()
                .map(s -> Signature.builder()
                        .contractId(contractId)
                        .contractTitle(setup.req().contractTitle())
                        .fileId(setup.req().fileId())
                        .fileStorageKey(setup.req().fileStorageKey())
                        .fileName(setup.req().fileName())
                        .organizationId(orgId)
                        .signerName(s.name())
                        .signerEmail(s.email())
                        .status(SignatureStatus.PENDING)
                        .token(SignatureTokenGenerator.newToken())
                        .providerName(providerRes.providerName())
                        .externalRef(providerRes.signerExternalRefs().get(s.email()))
                        .expiresAt(expiresAt)
                        .batchId(batchId)
                        .build())
                .toList();
        signatureRepository.saveAll(created);

        for (Signature s : created) {
            eventPublisher.publishSignatureRequested(
                    orgId, null,
                    new SignatureRequestedPayload(
                            contractId,
                            setup.req().contractTitle(),
                            s.getSignerName(),
                            s.getSignerEmail(),
                            s.getToken(),
                            s.getExpiresAt()
                    ));
        }

        log.info("Send-for-signature: contractId={}, signerCount={}", contractId, created.size());
        return created.stream().map(mapper::toResponse).toList();
    }
}
