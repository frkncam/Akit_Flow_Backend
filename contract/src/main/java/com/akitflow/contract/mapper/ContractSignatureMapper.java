package com.akitflow.contract.mapper;

import com.akitflow.contract.domain.ContractSignature;
import com.akitflow.contract.dto.response.ContractSignatureResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ContractSignatureMapper {

    @Mapping(target = "contractId", source = "contract.id")
    @Mapping(target = "fileId", source = "file.id")
    ContractSignatureResponse toResponse(ContractSignature entity);
}
