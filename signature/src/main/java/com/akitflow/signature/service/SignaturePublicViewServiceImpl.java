package com.akitflow.signature.service;

import com.akitflow.signature.dto.response.SignatureResponse;
import com.akitflow.signature.dto.response.SignatureViewResponse;
import com.akitflow.signature.exception.ResourceNotFoundException;
import com.akitflow.signature.exception.SignatureExpiredException;
import com.akitflow.signature.mapper.SignatureMapper;
import com.akitflow.signature.repository.SignatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignaturePublicViewServiceImpl implements SignaturePublicViewService {

    private final SignatureRepository signatureRepository;
    private final MinioService minioService;
    private final SignatureMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public SignatureViewResponse getByToken(String token) {
        var sig = signatureRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("İmza linki geçersiz"));
        if (sig.getExpiresAt().isBefore(Instant.now())) {
            throw new SignatureExpiredException();
        }
        String pdfUrl = minioService.presignedGetUrl(sig.getFileStorageKey());
        String signedPdfUrl = sig.getSignedFileStorageKey() != null
                ? minioService.presignedGetUrl(sig.getSignedFileStorageKey())
                : null;
        return new SignatureViewResponse(
                sig.getContractTitle(),
                sig.getSignerName(),
                sig.getSignerEmail(),
                pdfUrl,
                signedPdfUrl,
                sig.getExpiresAt(),
                sig.getStatus(),
                sig.getSignatureMetadata()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignatureResponse> listForContract(Long contractId, Long organizationId) {
        return signatureRepository.findAllByContractIdAndOrganizationId(contractId, organizationId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
