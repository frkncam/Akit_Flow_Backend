package com.akitflow.signature.service;

import com.akitflow.common.client.dto.BatchSignatureRequest;
import com.akitflow.common.client.dto.SignatureDto;

import java.util.List;

public interface SignatureRequestService {

    List<SignatureDto> sendForSignature(BatchSignatureRequest request, Long organizationId);
}
