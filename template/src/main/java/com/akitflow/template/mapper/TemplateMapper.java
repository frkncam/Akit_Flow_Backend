package com.akitflow.template.mapper;

import com.akitflow.template.domain.Template;
import com.akitflow.template.domain.TemplateVariableData;
import com.akitflow.template.dto.common.TemplateVariableDto;
import com.akitflow.template.dto.request.TemplateCreateRequest;
import com.akitflow.template.dto.request.TemplateUpdateRequest;
import com.akitflow.template.dto.response.TemplateResponse;
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
public interface TemplateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Template toEntity(TemplateCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Template entity, TemplateUpdateRequest request);

    TemplateResponse toResponse(Template entity);

    TemplateVariableData toData(TemplateVariableDto dto);

    TemplateVariableDto toDto(TemplateVariableData data);

    List<TemplateVariableData> toDataList(List<TemplateVariableDto> dtos);

    List<TemplateVariableDto> toDtoList(List<TemplateVariableData> data);
}
