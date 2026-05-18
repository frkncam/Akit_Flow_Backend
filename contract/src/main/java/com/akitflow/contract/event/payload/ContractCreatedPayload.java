package com.akitflow.contract.event.payload;

import com.akitflow.contract.domain.enums.ContractType;

import java.util.List;

public record ContractCreatedPayload(
        Long contractId,
        String title,
        ContractType contractType,
        List<String> partyEmails,
        String creatorEmail
) {}
