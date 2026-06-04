package com.akitflow.signature.service;

import com.akitflow.common.client.dto.SignatureDto;
import com.akitflow.common.event.payload.SignatureRequestedPayload;
import com.akitflow.signature.domain.Signature;
import com.akitflow.signature.domain.enums.SignatureStatus;
import com.akitflow.signature.event.publisher.SignatureEventPublisher;
import com.akitflow.signature.mapper.SignatureMapper;
import com.akitflow.signature.provider.ProviderSignatureResult;
import com.akitflow.signature.repository.SignatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignaturePersistenceService {

    private final SignatureRepository signatureRepository;
    private final SignatureMapper mapper;
    private final SignatureEventPublisher eventPublisher;

    @Transactional
    public List<SignatureDto> persist(SignatureRequestServiceImpl.SignatureSetup setup,
                                       ProviderSignatureResult providerRes) {
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
                        .documentHash(setup.documentHash())
                        .build())
                .toList();
        signatureRepository.saveAll(created);

        for (Signature s : created) {
            eventPublisher.publishSignatureRequested(
                    orgId, setup.actorId(),
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
        return created.stream().map(mapper::toDto).toList();
    }
}
