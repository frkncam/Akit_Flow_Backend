package com.akitflow.signature.service;

import com.akitflow.signature.dto.response.SignatureResponse;
import com.akitflow.signature.dto.response.SignatureViewResponse;

import java.util.List;

public interface SignaturePublicViewService {

    SignatureViewResponse getByToken(String token);

    List<SignatureResponse> listForContract(Long contractId);
}
