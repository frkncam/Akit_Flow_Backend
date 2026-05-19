package com.akitflow.contract.service;

import com.akitflow.contract.dto.response.ContractSignatureResponse;
import com.akitflow.contract.dto.response.SignatureViewResponse;
import com.akitflow.contract.exception.ResourceNotFoundException;
import com.akitflow.contract.exception.SignatureExpiredException;
import com.akitflow.contract.mapper.ContractSignatureMapper;
import com.akitflow.contract.repository.ContractRepository;
import com.akitflow.contract.repository.ContractSignatureRepository;
import com.akitflow.contract.security.HeaderPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignaturePublicViewService {

    private final ContractSignatureRepository signatureRepository;
    private final ContractRepository contractRepository;
    private final MinioService minioService;
    private final ContractSignatureMapper mapper;

    @Transactional(readOnly = true)
    public SignatureViewResponse getByToken(String token) {
        var sig = signatureRepository.findByToken(token)
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

    @Transactional(readOnly = true)
    public List<ContractSignatureResponse> listForContract(Long contractId, HeaderPrincipal user) {
        contractRepository.findByIdAndOrganizationId(contractId, user.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Sözleşme bulunamadı: id=" + contractId));
        return signatureRepository.findAllByContract_Id(contractId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
