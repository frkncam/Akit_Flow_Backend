package com.akitflow.contract.mapper;

import com.akitflow.contract.domain.ContractTemplate;
import com.akitflow.contract.domain.TemplateVariableData;
import com.akitflow.contract.dto.common.TemplateVariableDto;
import com.akitflow.contract.dto.request.ContractTemplateCreateRequest;
import com.akitflow.contract.dto.request.ContractTemplateUpdateRequest;
import com.akitflow.contract.dto.response.ContractTemplateResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ContractTemplateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ContractTemplate toEntity(ContractTemplateCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget ContractTemplate entity, ContractTemplateUpdateRequest request);

    ContractTemplateResponse toResponse(ContractTemplate entity);

    TemplateVariableData toData(TemplateVariableDto dto);

    TemplateVariableDto toDto(TemplateVariableData data);

    List<TemplateVariableData> toDataList(List<TemplateVariableDto> dtos);

    List<TemplateVariableDto> toDtoList(List<TemplateVariableData> data);
}
