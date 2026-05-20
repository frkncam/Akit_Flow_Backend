package com.akitflow.contract.exception;

import com.akitflow.contract.domain.enums.ContractStatus;

public class InvalidContractStateTransitionException extends RuntimeException {

    public InvalidContractStateTransitionException(ContractStatus from, ContractStatus to) {
        super("Invalid state transition: " + from + " → " + to);
    }
}
