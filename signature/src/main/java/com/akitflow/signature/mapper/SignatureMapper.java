package com.akitflow.signature.mapper;

import com.akitflow.common.client.dto.SignatureDto;
import com.akitflow.signature.domain.Signature;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {com.akitflow.signature.domain.enums.SignatureStatus.class}
)
public interface SignatureMapper {

    @Mapping(target = "status", expression = "java(entity.getStatus() != null ? entity.getStatus().name() : null)")
    SignatureDto toDto(Signature entity);
}
