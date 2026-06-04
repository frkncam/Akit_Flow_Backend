package com.muhur.notification.mapper;

import com.muhur.notification.domain.EmailLog;
import com.muhur.notification.dto.response.EmailLogResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmailLogMapper {

    EmailLogResponse toResponse(EmailLog entity);

    List<EmailLogResponse> toResponseList(List<EmailLog> entities);
}
