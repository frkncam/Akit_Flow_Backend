package com.akitflow.signature.mapper;

import com.akitflow.signature.domain.Signature;
import com.akitflow.signature.dto.response.SignatureResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface SignatureMapper {

    @Mapping(target = "contractId", source = "contractId")
    @Mapping(target = "fileId", source = "fileId")
    SignatureResponse toResponse(Signature entity);
}
