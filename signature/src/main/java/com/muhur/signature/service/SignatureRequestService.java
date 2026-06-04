package com.muhur.signature.service;

import com.muhur.common.client.dto.BatchSignatureRequest;
import com.muhur.common.client.dto.SignatureDto;

import java.util.List;

public interface SignatureRequestService {

    List<SignatureDto> sendForSignature(BatchSignatureRequest request, Long organizationId, Long actorId);
}
