package com.muhur.contract.mapper;

import com.muhur.contract.domain.ContractFile;
import com.muhur.contract.dto.response.ContractFileResponse;
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
