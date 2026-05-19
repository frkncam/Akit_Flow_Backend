package com.akitflow.contract.service;

import com.akitflow.contract.config.AppProperties;
import com.akitflow.contract.domain.Contract;
import com.akitflow.contract.domain.ContractFile;
import com.akitflow.contract.domain.ContractSignature;
import com.akitflow.contract.domain.enums.ContractStatus;
import com.akitflow.contract.domain.enums.SignatureStatus;
import com.akitflow.contract.dto.request.SendForSignatureRequest;
import com.akitflow.contract.dto.response.ContractSignatureResponse;
import com.akitflow.contract.event.payload.ContractSignatureRequestedPayload;
import com.akitflow.contract.event.publisher.ContractEventPublisher;
import com.akitflow.contract.exception.InvalidContractStateTransitionException;
import com.akitflow.contract.exception.ResourceNotFoundException;
import com.akitflow.contract.mapper.ContractSignatureMapper;
import com.akitflow.contract.repository.ContractFileRepository;
import com.akitflow.contract.repository.ContractRepository;
import com.akitflow.contract.repository.ContractSignatureRepository;
import com.akitflow.contract.security.HeaderPrincipal;
import com.akitflow.contract.signature.provider.ESignatureProvider;
import com.akitflow.contract.signature.provider.ProviderSignatureResult;
import com.akitflow.contract.signature.provider.SignatureRequest;
import com.akitflow.contract.signature.provider.SignerInfo;
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

    private final ContractRepository contractRepository;
    private final ContractFileRepository fileRepository;
    private final ContractSignatureRepository signatureRepository;
    private final ContractSignatureMapper mapper;
    private final ESignatureProvider provider;
    private final MinioService minioService;
    private final ContractEventPublisher eventPublisher;
    private final AppProperties appProperties;

    @Override
    public List<ContractSignatureResponse> sendForSignature(
            Long contractId,
            SendForSignatureRequest request,
            HeaderPrincipal user) {

        SignatureSetup setup = prepare(contractId, request, user);
        ProviderSignatureResult providerRes = provider.requestSignatures(setup.toProviderRequest());
        return persist(setup, providerRes);
    }

    private SignatureSetup prepare(
            Long contractId,
            SendForSignatureRequest request,
            HeaderPrincipal user) {

        Long orgId = user.organizationId();

        Contract contract = contractRepository.findByIdAndOrganizationId(contractId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Sözleşme bulunamadı: id=" + contractId));

        if (contract.getStatus() != ContractStatus.DRAFT
                && contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
            throw new InvalidContractStateTransitionException(
                    contract.getStatus(), ContractStatus.PENDING_SIGNATURE);
        }

        ContractFile file = fileRepository.findById(request.fileId())
                .orElseThrow(() -> new ResourceNotFoundException("Dosya bulunamadı: id=" + request.fileId()));
        if (!file.getContract().getId().equals(contractId)) {
            throw new ResourceNotFoundException("Dosya bu sözleşmeye ait değil: id=" + request.fileId());
        }

        byte[] pdf = minioService.download(file.getStorageKey());
        Duration validity = Duration.ofDays(appProperties.signature().token().validityDays());

        List<SignerInfo> signers = request.signers().stream()
                .map(s -> new SignerInfo(s.name(), s.email()))
                .toList();

        return new SignatureSetup(contract, file, pdf, signers, validity, orgId, user.userId());
    }

    private record SignatureSetup(
            Contract contract,
            ContractFile file,
            byte[] pdf,
            List<SignerInfo> signers,
            Duration validity,
            Long orgId,
            Long userId
    ) {
        SignatureRequest toProviderRequest() {
            return new SignatureRequest(
                    contract.getTitle(),
                    file.getFileName(),
                    pdf,
                    signers,
                    validity
            );
        }
    }

    @Transactional
    /* package-private */ List<ContractSignatureResponse> persist(
            SignatureSetup setup, ProviderSignatureResult providerRes) {

        Long contractId = setup.contract().getId();

        signatureRepository.findAllByContract_IdAndStatus(contractId, SignatureStatus.PENDING)
                .forEach(s -> s.setStatus(SignatureStatus.CANCELLED));

        UUID batchId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(setup.validity());

        List<ContractSignature> created = setup.signers().stream()
                .map(s -> ContractSignature.builder()
                        .contract(setup.contract())
                        .file(setup.file())
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

        setup.contract().setStatus(ContractStatus.PENDING_SIGNATURE);
        contractRepository.save(setup.contract());

        for (ContractSignature s : created) {
            eventPublisher.publishSignatureRequested(
                    setup.orgId(), setup.userId(),
                    new ContractSignatureRequestedPayload(
                            setup.contract().getId(),
                            setup.contract().getTitle(),
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
