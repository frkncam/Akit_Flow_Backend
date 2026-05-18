package com.akitflow.contract.service;

import com.akitflow.contract.domain.Contract;
import com.akitflow.contract.domain.ContractFile;
import com.akitflow.contract.domain.ContractSignature;
import com.akitflow.contract.domain.enums.ContractStatus;
import com.akitflow.contract.domain.enums.SignatureStatus;
import com.akitflow.contract.dto.request.SendForSignatureRequest;
import com.akitflow.contract.dto.response.ContractSignatureResponse;
import com.akitflow.contract.dto.response.SignatureViewResponse;
import com.akitflow.contract.event.payload.ContractSignatureRejectedPayload;
import com.akitflow.contract.event.payload.ContractSignatureRequestedPayload;
import com.akitflow.contract.event.payload.ContractSignedPayload;
import com.akitflow.contract.event.publisher.ContractEventPublisher;
import com.akitflow.contract.exception.InvalidContractStateTransitionException;
import com.akitflow.contract.exception.ResourceNotFoundException;
import com.akitflow.contract.exception.SignatureExpiredException;
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
public class ContractSignatureService {

    private static final Duration SIGNATURE_VALIDITY = Duration.ofDays(7);

    private final ContractRepository contractRepository;
    private final ContractFileRepository fileRepository;
    private final ContractSignatureRepository signatureRepository;
    private final ContractSignatureMapper mapper;
    private final ESignatureProvider provider;
    private final MinioService minioService;
    private final CertificateService certificateService;
    private final PdfSigningService pdfSigningService;
    private final ContractEventPublisher eventPublisher;

    // --- Authenticated (gateway header'lı) ----------------------------------

    @Transactional
    public List<ContractSignatureResponse> sendForSignature(
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

        // Eski PENDING signature'ları CANCELLED yap (yeniden gönderim senaryosu)
        signatureRepository.findAllByContract_IdAndStatus(contractId, SignatureStatus.PENDING)
                .forEach(s -> s.setStatus(SignatureStatus.CANCELLED));

        // Bu round'un tüm signature'ları aynı batch_id'yi paylaşır → accept/reject
        // kontrolleri eski round'ların REJECTED/CANCELLED kayıtlarına bakmaz
        UUID batchId = UUID.randomUUID();

        // Provider çağrısı
        byte[] pdf = minioService.download(file.getStorageKey());
        SignatureRequest providerReq = new SignatureRequest(
                contract.getTitle(),
                file.getFileName(),
                pdf,
                request.signers().stream()
                        .map(s -> new SignerInfo(s.name(), s.email()))
                        .toList(),
                SIGNATURE_VALIDITY
        );
        ProviderSignatureResult providerRes = provider.requestSignatures(providerReq);

        // Signature kayıtları oluştur
        Instant expiresAt = Instant.now().plus(SIGNATURE_VALIDITY);
        List<ContractSignature> created = request.signers().stream()
                .map(s -> ContractSignature.builder()
                        .contract(contract)
                        .file(file)
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

        // Contract status'u PENDING_SIGNATURE'a al
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        contractRepository.save(contract);

        // Her signer için ayrı event (notification mail kişiselleştirsin)
        for (ContractSignature s : created) {
            eventPublisher.publishSignatureRequested(
                    orgId, user.userId(),
                    new ContractSignatureRequestedPayload(
                            contract.getId(),
                            contract.getTitle(),
                            s.getSignerName(),
                            s.getSignerEmail(),
                            s.getToken(),
                            s.getExpiresAt()
                    ));
        }

        log.info("Send-for-signature: contractId={}, signerCount={}", contractId, created.size());
        return created.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ContractSignatureResponse> listForContract(Long contractId, HeaderPrincipal user) {
        // Org-scope kontrolü için contract'a ulaş
        contractRepository.findByIdAndOrganizationId(contractId, user.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Sözleşme bulunamadı: id=" + contractId));
        return signatureRepository.findAllByContract_Id(contractId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    // --- Public (token-based, JWT yok) --------------------------------------

    @Transactional(readOnly = true)
    public SignatureViewResponse getByToken(String token) {
        ContractSignature sig = signatureRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("İmza linki geçersiz"));
        if (sig.getExpiresAt().isBefore(Instant.now())) {
            throw new SignatureExpiredException();
        }
        String pdfUrl = minioService.presignedGetUrl(sig.getFile().getStorageKey());
        String signedPdfUrl = sig.getSignedFileStorageKey() != null
                ? minioService.presignedGetUrl(sig.getSignedFileStorageKey())
                : null;
        return new SignatureViewResponse(
                sig.getContract().getTitle(),
                sig.getSignerName(),
                sig.getSignerEmail(),
                pdfUrl,
                signedPdfUrl,
                sig.getExpiresAt(),
                sig.getStatus(),
                sig.getSignatureMetadata()
        );
    }

    @Transactional
    public void accept(String token) {
        ContractSignature sig = loadValidPending(token);
        sig.setStatus(SignatureStatus.SIGNED);
        sig.setSignedAt(Instant.now());

        // PDF'i imzala
        try {
            signPdf(sig);
        } catch (Exception e) {
            log.error("PDF imzalama başarısız, imza statüsü SIGNED olarak kaldı: {}", e.getMessage(), e);
        }

        signatureRepository.save(sig);

        Contract contract = sig.getContract();
        Long contractId = contract.getId();
        UUID batchId = sig.getBatchId();

        // Sadece bu round'un (batch'in) durumunu değerlendir; eski round'ların
        // REJECTED/CANCELLED kayıtları current round'u kirletmesin.
        List<ContractSignature> batch =
                signatureRepository.findAllByContract_IdAndBatchId(contractId, batchId);

        boolean anyPending  = batch.stream().anyMatch(s -> s.getStatus() == SignatureStatus.PENDING);
        boolean anyRejected = batch.stream().anyMatch(s -> s.getStatus() == SignatureStatus.REJECTED);
        boolean allSigned   = !anyPending
                && !anyRejected
                && batch.stream().anyMatch(s -> s.getStatus() == SignatureStatus.SIGNED);

        if (allSigned) {
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setSignedAt(Instant.now());
            contractRepository.save(contract);

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
            log.info("Contract fully signed: contractId={}, batchId={}", contractId, batchId);
        }
    }

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

        // Kalan PENDING'leri iptal et — sadece bu batch içinde.
        signatureRepository.findAllByContract_IdAndBatchIdAndStatus(
                        contractId, batchId, SignatureStatus.PENDING)
                .forEach(s -> s.setStatus(SignatureStatus.CANCELLED));

        // Contract DRAFT'a geri dön
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

    private void signPdf(ContractSignature sig) {
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
            throw new IllegalStateException("PDF imzalanamadı: " + e.getMessage(), e);
        }

        String signedKey = file.getStorageKey() + ".signed." + sig.getId();
        minioService.upload(signedKey,
                new java.io.ByteArrayInputStream(signedPdf),
                signedPdf.length,
                "application/pdf");
        sig.setSignedFileStorageKey(signedKey);

        sig.setSignatureMetadata(
                "{\"signedAt\":\"" + sig.getSignedAt() + "\"," +
                "\"signerName\":\"" + escapeJson(sig.getSignerName()) + "\"," +
                "\"signerEmail\":\"" + escapeJson(sig.getSignerEmail()) + "\"," +
                "\"algorithm\":\"SHA256withRSA\"," +
                "\"certificateSerial\":\"" + certKey.certificate().getSerialNumber().toString(16) + "\"}"
        );

        log.info("PDF signed for signature id={}, storageKey={}", sig.getId(), signedKey);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

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
