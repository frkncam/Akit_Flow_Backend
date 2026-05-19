package com.akitflow.contract.service;

import com.akitflow.contract.dto.response.ContractFileResponse;
import com.akitflow.contract.security.HeaderPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ContractFileService {

    ContractFileResponse upload(Long contractId, MultipartFile file, HeaderPrincipal user);

    List<ContractFileResponse> list(Long contractId, HeaderPrincipal user);

    ContractFileResponse getOne(Long fileId, HeaderPrincipal user);
}
