package com.muhur.contract.service;

import com.muhur.contract.dto.response.ContractFileResponse;
import com.muhur.common.security.HeaderPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ContractFileService {

    ContractFileResponse upload(Long contractId, MultipartFile file, HeaderPrincipal user);

    List<ContractFileResponse> list(Long contractId, HeaderPrincipal user);

    ContractFileResponse getOne(Long fileId, HeaderPrincipal user);
}
