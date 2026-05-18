package com.akitflow.contract.mapper;

import com.akitflow.contract.domain.ContractFile;
import com.akitflow.contract.dto.response.ContractFileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ContractFileMapper {

    @Mapping(target = "contractId", source = "contract.id")
    @Mapping(target = "downloadUrl", ignore = true)
    ContractFileResponse toResponse(ContractFile entity);
}
