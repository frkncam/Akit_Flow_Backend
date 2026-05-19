package com.akitflow.signature.service;

import com.akitflow.signature.SignatureMetadata;
import com.akitflow.signature.domain.Signature;
import com.akitflow.signature.domain.enums.SignatureStatus;
import com.akitflow.signature.event.payload.SignatureBatchCompletedPayload;
import com.akitflow.signature.event.payload.SignatureBatchRejectedPayload;
import com.akitflow.signature.event.publisher.SignatureEventPublisher;
import com.akitflow.signature.exception.PdfSigningFailedException;
import com.akitflow.signature.exception.ResourceNotFoundException;
import com.akitflow.signature.exception.SignatureExpiredException;
import com.akitflow.signature.repository.SignatureRepository;
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

    private final SignatureRepository signatureRepository;
    private final CertificateService certificateService;
    private final PdfSigningService pdfSigningService;
    private final MinioService minioService;
    private final ObjectMapper objectMapper;
    private final SignatureEventPublisher eventPublisher;
    private final SignatureBatchEvaluator batchEvaluator;

    @Override
    @Transactional
    public void accept(String token) {
        Signature sig = loadValidPending(token);
        SignedPdfResult signed = signPdfOrThrow(sig);

        sig.setStatus(SignatureStatus.SIGNED);
        sig.setSignedAt(signed.signedAt());
        sig.setSignedFileStorageKey(signed.storageKey());
        sig.setSignatureMetadata(signed.metadataJson());
        signatureRepository.save(sig);

        SignatureBatchEvaluator.BatchStatus status =
                batchEvaluator.evaluate(sig.getContractId(), sig.getBatchId());

        if (status.allSigned()) {
            List<Signature> batch = signatureRepository
                    .findAllByContractIdAndBatchId(sig.getContractId(), sig.getBatchId());
            List<String> signerEmails = batch.stream()
                    .filter(s -> s.getStatus() == SignatureStatus.SIGNED)
                    .map(Signature::getSignerEmail)
                    .toList();

            eventPublisher.publishBatchCompleted(
                    sig.getOrganizationId(),
                    null,
                    new SignatureBatchCompletedPayload(
                            sig.getContractId(),
                            sig.getContractTitle(),
                            null,
                            signerEmails
                    ));
            log.info("Signature batch completed: contractId={}, batchId={}",
                    sig.getContractId(), sig.getBatchId());
        }
    }

    @Override
    @Transactional
    public void reject(String token, String reason) {
        Signature sig = loadValidPending(token);
        sig.setStatus(SignatureStatus.REJECTED);
        sig.setRejectedAt(Instant.now());
        sig.setRejectionReason(reason);
        signatureRepository.save(sig);

        Long contractId = sig.getContractId();
        UUID batchId = sig.getBatchId();

        signatureRepository.findAllByContractIdAndBatchIdAndStatus(
                        contractId, batchId, SignatureStatus.PENDING)
                .forEach(s -> s.setStatus(SignatureStatus.CANCELLED));

        eventPublisher.publishBatchRejected(
                sig.getOrganizationId(),
                null,
                new SignatureBatchRejectedPayload(
                        contractId,
                        sig.getContractTitle(),
                        null,
                        sig.getSignerName(),
                        sig.getSignerEmail(),
                        reason
                ));
        log.info("Signature rejected: contractId={}, batchId={}, signer={}",
                contractId, batchId, sig.getSignerEmail());
    }

    private SignedPdfResult signPdfOrThrow(Signature sig) {
        CertificateService.CertKeyPair certKey = certificateService
                .getOrCreateCertificate(sig.getOrganizationId());

        byte[] originalPdf = minioService.download(sig.getFileStorageKey());
        byte[] signedPdf;
        try {
            signedPdf = pdfSigningService.sign(originalPdf, certKey.certificate(),
                    certKey.privateKey(), sig.getSignerName(), sig.getContractTitle());
        } catch (Exception e) {
            throw new PdfSigningFailedException("PDF imzalanamadı: " + e.getMessage(), e);
        }

        String signedKey = "signed-pdfs/" + sig.getContractId() + "/" + sig.getId() + ".pdf";
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

    private Signature loadValidPending(String token) {
        Signature sig = signatureRepository.findByToken(token)
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
