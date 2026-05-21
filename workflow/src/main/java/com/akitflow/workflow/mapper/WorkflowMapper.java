package com.akitflow.workflow.mapper;

import com.akitflow.workflow.domain.ApprovalStep;
import com.akitflow.workflow.domain.WorkflowTransition;
import com.akitflow.workflow.dto.response.ApprovalStepResponse;
import com.akitflow.workflow.dto.response.WorkflowTransitionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkflowMapper {

    ApprovalStepResponse toStepResponse(ApprovalStep step);

    WorkflowTransitionResponse toTransitionResponse(WorkflowTransition transition);
}
