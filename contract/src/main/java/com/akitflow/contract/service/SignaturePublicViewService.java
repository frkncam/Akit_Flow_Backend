package com.akitflow.contract.service;

import com.akitflow.contract.dto.response.ContractSignatureResponse;
import com.akitflow.contract.dto.response.SignatureViewResponse;
import com.akitflow.contract.security.HeaderPrincipal;

import java.util.List;

public interface SignaturePublicViewService {

    SignatureViewResponse getByToken(String token);

    List<ContractSignatureResponse> listForContract(Long contractId, HeaderPrincipal user);
}
