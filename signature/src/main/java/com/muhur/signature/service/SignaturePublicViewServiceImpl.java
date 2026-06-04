package com.muhur.signature.service;

import com.muhur.common.client.dto.SignatureDto;
import com.muhur.common.exception.ResourceNotFoundException;
import com.muhur.signature.dto.response.SignatureViewResponse;
import com.muhur.signature.exception.SignatureExpiredException;
import com.muhur.signature.mapper.SignatureMapper;
import com.muhur.signature.repository.SignatureRepository;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignaturePublicViewServiceImpl implements SignaturePublicViewService {

    private final SignatureRepository signatureRepository;
    private final MinioService minioService;
    private final SignatureMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public SignatureViewResponse getByToken(String token) {
        var sig = signatureRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid signature link"));
        if (sig.getExpiresAt().isBefore(Instant.now())) {
            throw new SignatureExpiredException();
        }
        String pdfUrl = minioService.presignedGetUrl(sig.getFileStorageKey());
        String signedPdfUrl = sig.getSignedFileStorageKey() != null
                ? minioService.presignedGetUrl(sig.getSignedFileStorageKey())
                : null;

        boolean otpRequired = sig.getStatus() == com.muhur.signature.domain.enums.SignatureStatus.PENDING;
        boolean otpVerified = sig.getOtpVerifiedAt() != null;

        return new SignatureViewResponse(
                sig.getContractTitle(),
                sig.getSignerName(),
                sig.getSignerEmail(),
                pdfUrl,
                signedPdfUrl,
                sig.getExpiresAt(),
                sig.getStatus(),
                sig.getSignatureMetadata(),
                otpRequired,
                otpVerified
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignatureDto> listForContract(Long contractId, Long organizationId) {
        return signatureRepository.findAllByContractIdAndOrganizationId(contractId, organizationId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SignatureDto> search(Predicate predicate, Pageable pageable) {
        return signatureRepository.findAll(predicate, pageable).map(mapper::toDto);
    }
}
