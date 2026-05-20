package com.akitflow.contract.service;

import com.akitflow.contract.domain.Contract;
import com.akitflow.contract.domain.ContractFile;
import com.akitflow.contract.dto.response.ContractFileResponse;
import com.akitflow.common.exception.ResourceNotFoundException;
import com.akitflow.contract.mapper.ContractFileMapper;
import com.akitflow.contract.repository.ContractFileRepository;
import com.akitflow.contract.repository.ContractRepository;
import com.akitflow.common.security.HeaderPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractFileServiceImpl implements ContractFileService {

    private final ContractRepository contractRepository;
    private final ContractFileRepository fileRepository;
    private final ContractFileMapper fileMapper;
    private final MinioService minioService;

    @Override
    @Transactional
    public ContractFileResponse upload(Long contractId, MultipartFile file, HeaderPrincipal user) {
        Long orgId = user.organizationId();
        Long userId = user.userId();

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Boş dosya yüklenemez.");
        }

        Contract contract = contractRepository.findByIdAndOrganizationId(contractId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: id=" + contractId));

        int nextVersion = fileRepository.findMaxVersionByContractId(contractId) + 1;
        String safeName = sanitize(file.getOriginalFilename());
        String storageKey = "%d/%d/v%d-%s".formatted(orgId, contractId, nextVersion, safeName);

        try {
            minioService.upload(storageKey, file.getInputStream(), file.getSize(),
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        } catch (IOException e) {
            throw new IllegalStateException("Dosya okunamadı: " + e.getMessage(), e);
        }

        ContractFile entity = ContractFile.builder()
                .contract(contract)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .size(file.getSize())
                .storageKey(storageKey)
                .version(nextVersion)
                .uploadedBy(userId)
                .build();
        entity = fileRepository.save(entity);

        log.info("File uploaded: contractId={}, fileName={}, version={}", contractId, file.getOriginalFilename(), nextVersion);

        return withDownloadUrl(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractFileResponse> list(Long contractId, HeaderPrincipal user) {
        contractRepository.findByIdAndOrganizationId(contractId, user.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: id=" + contractId));
        return fileRepository.findAllByContract_IdOrderByVersionDesc(contractId).stream()
                .map(this::withDownloadUrl)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractFileResponse getOne(Long fileId, HeaderPrincipal user) {
        ContractFile f = fileRepository.findByIdAndContract_OrganizationId(fileId, user.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("File not found: id=" + fileId));
        return withDownloadUrl(f);
    }

    private ContractFileResponse withDownloadUrl(ContractFile f) {
        ContractFileResponse base = fileMapper.toResponse(f);
        String url = minioService.presignedGetUrl(f.getStorageKey());
        return new ContractFileResponse(
                base.id(),
                base.contractId(),
                base.fileName(),
                base.contentType(),
                base.size(),
                base.version(),
                base.uploadedBy(),
                base.uploadedAt(),
                url
        );
    }

    private String sanitize(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
