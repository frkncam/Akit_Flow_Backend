package com.muhur.audit.mapper;

import com.muhur.audit.domain.AuditEvent;
import com.muhur.audit.dto.response.AuditEventResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditEventMapper {

    AuditEventResponse toResponse(AuditEvent entity);
}
