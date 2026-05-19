package com.akitflow.signature.service;

public interface SignatureDecisionService {

    void accept(String token);

    void reject(String token, String reason);
}
