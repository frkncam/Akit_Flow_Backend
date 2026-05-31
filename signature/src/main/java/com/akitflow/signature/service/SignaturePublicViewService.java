package com.akitflow.signature.service;

import com.akitflow.common.client.dto.SignatureDto;
import com.akitflow.signature.dto.response.SignatureViewResponse;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SignaturePublicViewService {

    SignatureViewResponse getByToken(String token);

    List<SignatureDto> listForContract(Long contractId, Long organizationId);

    Page<SignatureDto> search(Predicate predicate, Pageable pageable);
}
