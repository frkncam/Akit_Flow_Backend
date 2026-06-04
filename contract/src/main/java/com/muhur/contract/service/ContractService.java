package com.muhur.contract.service;

import com.muhur.contract.domain.enums.ContractStatus;
import com.muhur.contract.dto.request.ContractCreateRequest;
import com.muhur.contract.dto.request.ContractUpdateRequest;
import com.muhur.contract.dto.request.SendForSignatureRequest;
import com.muhur.contract.dto.response.ContractResponse;
import com.muhur.contract.dto.response.SignatureSummaryResponse;
import com.muhur.common.security.HeaderPrincipal;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ContractService {

    ContractResponse create(ContractCreateRequest request, HeaderPrincipal user);

    Page<ContractResponse> search(Predicate predicate, Pageable pageable, HeaderPrincipal user);

    ContractResponse get(Long id, HeaderPrincipal user);

    ContractResponse update(Long id, ContractUpdateRequest request, HeaderPrincipal user);

    ContractResponse updateStatus(Long id, ContractStatus newStatus, HeaderPrincipal user);

    void delete(Long id, HeaderPrincipal user);

    List<SignatureSummaryResponse> sendForSignature(Long id, SendForSignatureRequest request, HeaderPrincipal user);

    List<SignatureSummaryResponse> listSignatures(Long id, HeaderPrincipal user);
}
