package com.akitflow.workflow.service;

import com.akitflow.workflow.domain.WorkflowInstance;
import com.akitflow.workflow.domain.enums.WorkflowEvent;
import com.akitflow.workflow.domain.enums.WorkflowState;

public interface WorkflowStateMachineService {

    WorkflowState sendEvent(WorkflowInstance instance, WorkflowEvent event,
                            Long triggeredBy, String comment);
}
