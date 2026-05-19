package com.akitflow.contract.service;

import com.akitflow.contract.domain.enums.ContractStatus;
import com.akitflow.contract.dto.request.ContractCreateRequest;
import com.akitflow.contract.dto.request.ContractUpdateRequest;
import com.akitflow.contract.dto.response.ContractResponse;
import com.akitflow.contract.security.HeaderPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContractService {

    ContractResponse create(ContractCreateRequest request, HeaderPrincipal user);

    Page<ContractResponse> list(Pageable pageable, HeaderPrincipal user);

    ContractResponse get(Long id, HeaderPrincipal user);

    ContractResponse update(Long id, ContractUpdateRequest request, HeaderPrincipal user);

    ContractResponse updateStatus(Long id, ContractStatus newStatus, HeaderPrincipal user);

    void delete(Long id, HeaderPrincipal user);
}
