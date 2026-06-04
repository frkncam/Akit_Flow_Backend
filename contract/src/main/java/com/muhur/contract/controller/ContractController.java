package com.muhur.contract.controller;

import com.muhur.common.query.CommonPredicate;
import com.muhur.contract.domain.Contract;
import com.muhur.contract.dto.request.ContractCreateRequest;
import com.muhur.contract.dto.request.ContractStatusUpdateRequest;
import com.muhur.contract.dto.request.ContractUpdateRequest;
import com.muhur.contract.dto.request.SendForSignatureRequest;
import com.muhur.contract.dto.response.ContractResponse;
import com.muhur.contract.dto.response.PageResponse;
import com.muhur.contract.dto.response.SignatureSummaryResponse;
import com.muhur.common.security.HeaderPrincipal;
import com.muhur.contract.service.ContractService;
import com.querydsl.core.types.Predicate;
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

    @PostMapping
    public ResponseEntity<ContractResponse> create(@Valid @RequestBody ContractCreateRequest request,
                                                   @AuthenticationPrincipal HeaderPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.create(request, user));
    }

    @GetMapping
    public PageResponse<ContractResponse> list(
            @CommonPredicate(root = Contract.class) Predicate predicate,
            Pageable pageable,
            @AuthenticationPrincipal HeaderPrincipal user) {
        return PageResponse.from(contractService.search(predicate, pageable, user));
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
    public ResponseEntity<List<SignatureSummaryResponse>> sendForSignature(
            @PathVariable Long id,
            @Valid @RequestBody SendForSignatureRequest request,
            @AuthenticationPrincipal HeaderPrincipal user) {
        return ResponseEntity.ok(contractService.sendForSignature(id, request, user));
    }

    @GetMapping("/{id}/signatures")
    public List<SignatureSummaryResponse> listSignatures(
            @PathVariable Long id,
            @AuthenticationPrincipal HeaderPrincipal user) {
        return contractService.listSignatures(id, user);
    }
}
