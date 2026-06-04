package com.muhur.contract.exception;

import com.muhur.contract.domain.enums.ContractStatus;

public class InvalidContractStateTransitionException extends RuntimeException {

    public InvalidContractStateTransitionException(ContractStatus from, ContractStatus to) {
        super("Invalid state transition: " + from + " → " + to);
    }
}
