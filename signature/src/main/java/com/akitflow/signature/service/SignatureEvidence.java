package com.akitflow.signature.service;

public record SignatureEvidence(String ip, String userAgent, boolean consent) {}
