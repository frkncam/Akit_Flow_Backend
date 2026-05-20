package com.akitflow.signature.service;

import com.akitflow.common.client.dto.SignatureDto;
import com.akitflow.signature.dto.response.SignatureViewResponse;

import java.util.List;

public interface SignaturePublicViewService {

    SignatureViewResponse getByToken(String token);

    List<SignatureDto> listForContract(Long contractId, Long organizationId);
}
