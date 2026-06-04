package com.akitflow.signature.service;

public interface SignatureDecisionService {

    void accept(String token, SignatureEvidence evidence);

    void reject(String token, String reason);

    void requestOtp(String token);

    void verifyOtp(String token, String code);
}
