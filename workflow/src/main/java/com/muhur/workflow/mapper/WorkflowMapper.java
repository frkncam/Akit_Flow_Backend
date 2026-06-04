package com.muhur.workflow.mapper;

import com.muhur.workflow.domain.ApprovalStep;
import com.muhur.workflow.domain.WorkflowTransition;
import com.muhur.workflow.dto.response.ApprovalStepResponse;
import com.muhur.workflow.dto.response.WorkflowTransitionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkflowMapper {

    ApprovalStepResponse toStepResponse(ApprovalStep step);

    WorkflowTransitionResponse toTransitionResponse(WorkflowTransition transition);
}
