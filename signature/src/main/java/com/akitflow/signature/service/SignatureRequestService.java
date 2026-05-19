package com.akitflow.signature.service;

import com.akitflow.signature.dto.request.BatchSignatureRequest;
import com.akitflow.signature.dto.response.SignatureResponse;

import java.util.List;

public interface SignatureRequestService {

    List<SignatureResponse> sendForSignature(BatchSignatureRequest request, Long organizationId);
}
