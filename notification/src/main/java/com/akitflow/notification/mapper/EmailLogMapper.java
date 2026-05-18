package com.akitflow.notification.mapper;

import com.akitflow.notification.domain.EmailLog;
import com.akitflow.notification.dto.response.EmailLogResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmailLogMapper {

    EmailLogResponse toResponse(EmailLog entity);

    List<EmailLogResponse> toResponseList(List<EmailLog> entities);
}
