package com.muhur.workflow.service;

import com.muhur.workflow.domain.WorkflowInstance;
import com.muhur.workflow.domain.enums.WorkflowEvent;
import com.muhur.workflow.domain.enums.WorkflowState;

public interface WorkflowStateMachineService {

    WorkflowState sendEvent(WorkflowInstance instance, WorkflowEvent event,
                            Long triggeredBy, String comment);
}
