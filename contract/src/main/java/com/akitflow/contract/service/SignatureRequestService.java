package com.akitflow.contract.service;

import com.akitflow.contract.dto.request.SendForSignatureRequest;
import com.akitflow.contract.dto.response.ContractSignatureResponse;
import com.akitflow.contract.security.HeaderPrincipal;

import java.util.List;

public interface SignatureRequestService {

    List<ContractSignatureResponse> sendForSignature(Long contractId, SendForSignatureRequest request, HeaderPrincipal user);
}
