package com.akitflow.contract.service;

import com.akitflow.contract.domain.Contract;
import com.akitflow.contract.domain.ContractFile;
import com.akitflow.contract.domain.ContractSignature;
import com.akitflow.contract.domain.enums.ContractStatus;
import com.akitflow.contract.domain.enums.SignatureStatus;
import com.akitflow.contract.event.payload.ContractSignatureRejectedPayload;
import com.akitflow.contract.event.payload.ContractSignedPayload;
import com.akitflow.contract.event.publisher.ContractEventPublisher;
import com.akitflow.contract.exception.PdfSigningFailedException;
import com.akitflow.contract.exception.ResourceNotFoundException;
import com.akitflow.contract.exception.SignatureExpiredException;
import com.akitflow.contract.repository.ContractRepository;
import com.akitflow.contract.repository.ContractSignatureRepository;
import com.akitflow.contract.signature.SignatureMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class SignatureDecisionServiceImpl implements SignatureDecisionService {

    private final ContractSignatureRepository signatureRepository;
    private final ContractRepository contractRepository;
    private final CertificateService certificateService;
    private final PdfSigningService pdfSigningService;
    private final MinioService minioService;
    private final ObjectMapper objectMapper;
    private final ContractEventPublisher eventPublisher;
    private final SignatureBatchEvaluator batchEvaluator;

    @Override
    @Transactional
    public void accept(String token) {
        ContractSignature sig = loadValidPending(token);
        SignedPdfResult signed = signPdfOrThrow(sig);

        sig.setStatus(SignatureStatus.SIGNED);
        sig.setSignedAt(signed.signedAt());
        sig.setSignedFileStorageKey(signed.storageKey());
        sig.setSignatureMetadata(signed.metadataJson());
        signatureRepository.save(sig);

        SignatureBatchEvaluator.BatchStatus status =
                batchEvaluator.evaluate(sig.getContract().getId(), sig.getBatchId());

        if (status.allSigned()) {
            Contract contract = sig.getContract();
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setSignedAt(Instant.now());
            contractRepository.save(contract);

            List<ContractSignature> batch = signatureRepository
                    .findAllByContract_IdAndBatchId(contract.getId(), sig.getBatchId());
            List<String> signerEmails = batch.stream()
                    .filter(s -> s.getStatus() == SignatureStatus.SIGNED)
                    .map(ContractSignature::getSignerEmail)
                    .toList();

            eventPublisher.publishContractSigned(
                    contract.getOrganizationId(),
                    null,
                    new ContractSignedPayload(
                            contract.getId(),
                            contract.getTitle(),
                            contract.getCreatorEmail(),
                            signerEmails
                    ));
            log.info("Contract fully signed: contractId={}, batchId={}",
                    contract.getId(), sig.getBatchId());
        }
    }

    @Override
    @Transactional
    public void reject(String token, String reason) {
        ContractSignature sig = loadValidPending(token);
        sig.setStatus(SignatureStatus.REJECTED);
        sig.setRejectedAt(Instant.now());
        sig.setRejectionReason(reason);
        signatureRepository.save(sig);

        Contract contract = sig.getContract();
        Long contractId = contract.getId();
        UUID batchId = sig.getBatchId();

        signatureRepository.findAllByContract_IdAndBatchIdAndStatus(
                        contractId, batchId, SignatureStatus.PENDING)
                .forEach(s -> s.setStatus(SignatureStatus.CANCELLED));

        contract.setStatus(ContractStatus.DRAFT);
        contractRepository.save(contract);

        eventPublisher.publishSignatureRejected(
                contract.getOrganizationId(),
                null,
                new ContractSignatureRejectedPayload(
                        contract.getId(),
                        contract.getTitle(),
                        contract.getCreatorEmail(),
                        sig.getSignerName(),
                        sig.getSignerEmail(),
                        reason
                ));
        log.info("Signature rejected: contractId={}, batchId={}, signer={}",
                contractId, batchId, sig.getSignerEmail());
    }

    private SignedPdfResult signPdfOrThrow(ContractSignature sig) {
        Contract contract = sig.getContract();
        ContractFile file = sig.getFile();

        CertificateService.CertKeyPair certKey = certificateService
                .getOrCreateCertificate(contract.getOrganizationId());

        byte[] originalPdf = minioService.download(file.getStorageKey());
        byte[] signedPdf;
        try {
            signedPdf = pdfSigningService.sign(originalPdf, certKey.certificate(),
                    certKey.privateKey(), sig.getSignerName(), contract.getTitle());
        } catch (Exception e) {
            throw new PdfSigningFailedException("PDF imzalanamadı: " + e.getMessage(), e);
        }

        String signedKey = file.getStorageKey() + ".signed." + sig.getId();
        minioService.upload(signedKey,
                new java.io.ByteArrayInputStream(signedPdf),
                signedPdf.length,
                "application/pdf");

        Instant now = Instant.now();
        SignatureMetadata metadata = new SignatureMetadata(
                now,
                sig.getSignerName(),
                sig.getSignerEmail(),
                "SHA256withRSA",
                certKey.certificate().getSerialNumber().toString(16)
        );
        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            throw new PdfSigningFailedException("Metadata JSON oluşturulamadı", e);
        }

        log.info("PDF signed for signature id={}, storageKey={}", sig.getId(), signedKey);
        return new SignedPdfResult(now, signedKey, metadataJson);
    }

    private record SignedPdfResult(Instant signedAt, String storageKey, String metadataJson) {}

    private ContractSignature loadValidPending(String token) {
        ContractSignature sig = signatureRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("İmza linki geçersiz"));
        if (sig.getStatus() != SignatureStatus.PENDING) {
            throw new ResourceNotFoundException("İmza linki artık geçerli değil (durum: " + sig.getStatus() + ")");
        }
        if (sig.getExpiresAt().isBefore(Instant.now())) {
            throw new SignatureExpiredException();
        }
        return sig;
    }
}
