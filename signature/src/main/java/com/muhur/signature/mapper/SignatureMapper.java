package com.muhur.signature.mapper;

import com.muhur.common.client.dto.SignatureDto;
import com.muhur.signature.domain.Signature;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {com.muhur.signature.domain.enums.SignatureStatus.class}
)
public interface SignatureMapper {

    @Mapping(target = "status", expression = "java(entity.getStatus() != null ? entity.getStatus().name() : null)")
    SignatureDto toDto(Signature entity);
}
