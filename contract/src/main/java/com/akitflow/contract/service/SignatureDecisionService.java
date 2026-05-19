package com.akitflow.contract.service;

public interface SignatureDecisionService {

    void accept(String token);

    void reject(String token, String reason);
}
