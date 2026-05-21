package com.akitflow.audit.mapper;

import com.akitflow.audit.domain.AuditEvent;
import com.akitflow.audit.dto.response.AuditEventResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditEventMapper {

    AuditEventResponse toResponse(AuditEvent entity);
}
