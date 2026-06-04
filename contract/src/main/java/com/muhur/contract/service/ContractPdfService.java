package com.muhur.contract.service;

import com.muhur.contract.dto.request.GeneratePdfRequest;
import com.muhur.contract.dto.response.ContractFileResponse;
import com.muhur.common.security.HeaderPrincipal;

public interface ContractPdfService {

    ContractFileResponse generatePdf(Long contractId, Long templateId, GeneratePdfRequest body, HeaderPrincipal user);
}
