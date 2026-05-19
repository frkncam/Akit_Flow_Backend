package com.akitflow.contract.controller;

import com.akitflow.contract.client.SignatureClient;
import com.akitflow.contract.domain.Contract;
import com.akitflow.contract.domain.ContractFile;
import com.akitflow.contract.domain.enums.ContractStatus;
import com.akitflow.contract.dto.request.ContractCreateRequest;
import com.akitflow.contract.dto.request.ContractStatusUpdateRequest;
import com.akitflow.contract.dto.request.ContractUpdateRequest;
import com.akitflow.contract.dto.request.SendForSignatureRequest;
import com.akitflow.contract.dto.response.ContractResponse;
import com.akitflow.contract.dto.response.PageResponse;
import com.akitflow.contract.exception.InvalidContractStateTransitionException;
import com.akitflow.contract.exception.ResourceNotFoundException;
import com.akitflow.contract.repository.ContractFileRepository;
import com.akitflow.contract.repository.ContractRepository;
import com.akitflow.contract.security.HeaderPrincipal;
import com.akitflow.contract.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final ContractRepository contractRepository;
    private final ContractFileRepository fileRepository;
    private final SignatureClient signatureClient;

    @PostMapping
    public ResponseEntity<ContractResponse> create(@Valid @RequestBody ContractCreateRequest request,
                                                   @AuthenticationPrincipal HeaderPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.create(request, user));
    }

    @GetMapping
    public PageResponse<ContractResponse> list(Pageable pageable,
                                               @AuthenticationPrincipal HeaderPrincipal user) {
        return PageResponse.from(contractService.list(pageable, user));
    }

    @GetMapping("/{id}")
    public ContractResponse get(@PathVariable Long id,
                                @AuthenticationPrincipal HeaderPrincipal user) {
        return contractService.get(id, user);
    }

    @PatchMapping("/{id}")
    public ContractResponse update(@PathVariable Long id,
                                   @Valid @RequestBody ContractUpdateRequest request,
                                   @AuthenticationPrincipal HeaderPrincipal user) {
        return contractService.update(id, request, user);
    }

    @PatchMapping("/{id}/status")
    public ContractResponse updateStatus(@PathVariable Long id,
                                         @Valid @RequestBody ContractStatusUpdateRequest request,
                                         @AuthenticationPrincipal HeaderPrincipal user) {
        return contractService.updateStatus(id, request.status(), user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal HeaderPrincipal user) {
        contractService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/send-for-signature")
    public ResponseEntity<List<SignatureClient.SignatureDto>> sendForSignature(
            @PathVariable Long id,
            @Valid @RequestBody SendForSignatureRequest request,
            @AuthenticationPrincipal HeaderPrincipal user) {

        Contract contract = contractRepository.findByIdAndOrganizationId(id, user.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: id=" + id));

        if (contract.getStatus() != ContractStatus.DRAFT
                && contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
            throw new InvalidContractStateTransitionException(
                    contract.getStatus(), ContractStatus.PENDING_SIGNATURE);
        }

        ContractFile file = fileRepository.findById(request.fileId())
                .orElseThrow(() -> new ResourceNotFoundException("File not found: id=" + request.fileId()));
        if (!file.getContract().getId().equals(id)) {
            throw new ResourceNotFoundException("File does not belong to this contract: id=" + request.fileId());
        }

        var batchRequest = new SignatureClient.BatchSignatureRequest(
                contract.getId(),
                contract.getTitle(),
                file.getId(),
                file.getStorageKey(),
                file.getFileName(),
                user.organizationId(),
                request.signers().stream()
                        .map(s -> new SignatureClient.SignerRequest(s.name(), s.email()))
                        .toList()
        );

        return ResponseEntity.accepted().body(
                signatureClient.sendForSignature(batchRequest, user.userId(), user.organizationId(), user.email()));
    }

    @GetMapping("/{id}/signatures")
    public List<SignatureClient.SignatureDto> listSignatures(
            @PathVariable Long id,
            @AuthenticationPrincipal HeaderPrincipal user) {
        return signatureClient.listForContract(id, user.organizationId());
    }
}
