package com.akitflow.contract.service;

import com.akitflow.contract.dto.request.GeneratePdfRequest;
import com.akitflow.contract.dto.response.ContractFileResponse;
import com.akitflow.common.security.HeaderPrincipal;

public interface ContractPdfService {

    ContractFileResponse generatePdf(Long contractId, Long templateId, GeneratePdfRequest body, HeaderPrincipal user);
}
