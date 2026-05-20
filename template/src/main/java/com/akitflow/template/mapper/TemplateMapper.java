package com.akitflow.template.mapper;

import com.akitflow.common.client.dto.TemplateDto;
import com.akitflow.common.client.dto.TemplateVariableDto;
import com.akitflow.template.domain.Template;
import com.akitflow.template.domain.TemplateVariableData;
import com.akitflow.template.domain.TemplateVariableSource;
import com.akitflow.template.domain.TemplateVariableType;
import com.akitflow.template.dto.request.TemplateCreateRequest;
import com.akitflow.template.dto.request.TemplateUpdateRequest;
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

    @Mapping(target = "category", expression = "java(entity.getCategory() != null ? entity.getCategory().name() : null)")
    TemplateDto toDto(Template entity);

    @Mapping(target = "type", expression = "java(dto.type() != null ? TemplateVariableType.valueOf(dto.type()) : null)")
    @Mapping(target = "source", expression = "java(dto.source() != null ? TemplateVariableSource.valueOf(dto.source()) : null)")
    TemplateVariableData toData(TemplateVariableDto dto);

    @Mapping(target = "type", expression = "java(data.type() != null ? data.type().name() : null)")
    @Mapping(target = "source", expression = "java(data.source() != null ? data.source().name() : null)")
    TemplateVariableDto toDto(TemplateVariableData data);

    List<TemplateVariableData> toDataList(List<TemplateVariableDto> dtos);

    List<TemplateVariableDto> toDtoList(List<TemplateVariableData> data);
}
