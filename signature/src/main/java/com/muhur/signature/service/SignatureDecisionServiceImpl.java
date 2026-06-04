package com.muhur.signature.service;

import com.muhur.signature.SignatureMetadata;
import com.muhur.signature.config.AppProperties;
import com.muhur.signature.domain.Signature;
import com.muhur.signature.domain.enums.SignatureStatus;
import com.muhur.common.event.payload.SignatureBatchCompletedPayload;
import com.muhur.common.event.payload.SignatureBatchRejectedPayload;
import com.muhur.common.event.payload.SignatureOtpRequestedPayload;
import com.muhur.signature.event.publisher.SignatureEventPublisher;
import com.muhur.signature.exception.ConsentRequiredException;
import com.muhur.signature.exception.PdfSigningFailedException;
import com.muhur.common.exception.ResourceNotFoundException;
import com.muhur.signature.exception.SignatureExpiredException;
import com.muhur.signature.repository.SignatureRepository;
import com.muhur.signature.util.DocumentHasher;
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
    private final OtpService otpService;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void accept(String token, SignatureEvidence evidence) {
        Signature sig = loadValidPending(token);

        if (!evidence.consent()) {
            throw new ConsentRequiredException();
        }

        otpService.requireFreshlyVerified(sig);

        sig.setSignerIp(evidence.ip());
        sig.setSignerUserAgent(evidence.userAgent());
        sig.setConsentAcceptedAt(Instant.now());

        SignedPdfResult signed = signPdfOrThrow(sig);

        sig.setStatus(SignatureStatus.SIGNED);
        sig.setSignedAt(signed.signedAt());
        sig.setSignedFileStorageKey(signed.storageKey());
        sig.setSignatureMetadata(signed.metadataJson());
        sig.setTsaTime(signed.tsaTime());
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

    @Override
    @Transactional
    public void requestOtp(String token) {
        Signature sig = loadValidPending(token);
        otpService.ensureResendAllowed(sig);
        String code = otpService.generateAndStore(sig);
        signatureRepository.save(sig);

        AppProperties.Signature.Otp otpConfig = appProperties.signature().otp();
        eventPublisher.publishOtpRequested(
                sig.getOrganizationId(),
                null,
                new SignatureOtpRequestedPayload(
                        sig.getContractId(),
                        sig.getContractTitle(),
                        sig.getSignerName(),
                        sig.getSignerEmail(),
                        sig.getToken(),
                        code,
                        Instant.now().plusSeconds(otpConfig.ttlMinutes() * 60L)
                ));
        log.info("OTP requested for signature id={}, email={}", sig.getId(), sig.getSignerEmail());
    }

    @Override
    @Transactional
    public void verifyOtp(String token, String code) {
        Signature sig = loadValidPending(token);
        otpService.verify(sig, code);
        signatureRepository.save(sig);
        log.info("OTP verified for signature id={}", sig.getId());
    }

    private SignedPdfResult signPdfOrThrow(Signature sig) {
        CertificateService.CertKeyPair certKey = certificateService
                .getOrCreateCertificate(sig.getOrganizationId());

        byte[] originalPdf = minioService.download(sig.getFileStorageKey());

        String actualHash = DocumentHasher.sha256Hex(originalPdf);
        if (sig.getDocumentHash() != null && !sig.getDocumentHash().equals(actualHash)) {
            throw new PdfSigningFailedException("Document changed since request (hash mismatch)", null);
        }

        byte[] signedPdf;
        PdfSigningService.SignResult signResult;
        try {
            PdfSigningService.SignContext ctx = new PdfSigningService.SignContext(
                    sig.getSignerName(),
                    sig.getContractTitle(),
                    actualHash,
                    sig.getSignerEmail(),
                    sig.getSignerIp(),
                    sig.getOtpVerifiedAt() != null,
                    certKey.certificate().getSerialNumber().toString(16),
                    appProperties.signature().consent().text(),
                    appProperties.signature().certificate().algorithm()
            );
            signResult = pdfSigningService.sign(originalPdf, certKey.certificate(),
                    certKey.privateKey(), ctx);
            signedPdf = signResult.pdf();
        } catch (PdfSigningFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new PdfSigningFailedException("PDF signing failed: " + e.getMessage(), e);
        }

        String signedKey = "signed-pdfs/" + sig.getContractId() + "/" + sig.getId() + ".pdf";
        minioService.upload(signedKey,
                new java.io.ByteArrayInputStream(signedPdf),
                signedPdf.length,
                "application/pdf");

        Instant now = Instant.now();
        Instant effectiveTime = signResult.tsaTime() != null ? signResult.tsaTime() : now;
        SignatureMetadata metadata = new SignatureMetadata(
                effectiveTime,
                sig.getSignerName(),
                sig.getSignerEmail(),
                "SHA256withRSA",
                certKey.certificate().getSerialNumber().toString(16),
                actualHash,
                sig.getSignerIp(),
                sig.getSignerUserAgent(),
                sig.getOtpVerifiedAt() != null,
                appProperties.signature().consent().text(),
                signResult.tsaTime(),
                signResult.tsaAuthority()
        );
        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            throw new PdfSigningFailedException("Metadata JSON creation failed", e);
        }

        log.info("PDF signed for signature id={}, storageKey={}", sig.getId(), signedKey);
        return new SignedPdfResult(now, signedKey, metadataJson, signResult.tsaTime());
    }

    private record SignedPdfResult(Instant signedAt, String storageKey, String metadataJson, Instant tsaTime) {}

    private Signature loadValidPending(String token) {
        Signature sig = signatureRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid signature link"));
        if (sig.getStatus() != SignatureStatus.PENDING) {
            throw new ResourceNotFoundException("Signature link no longer valid (status: " + sig.getStatus() + ")");
        }
        if (sig.getExpiresAt().isBefore(Instant.now())) {
            throw new SignatureExpiredException();
        }
        return sig;
    }
}
