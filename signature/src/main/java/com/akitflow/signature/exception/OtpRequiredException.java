package com.akitflow.signature.exception;

public class OtpRequiredException extends SignatureException {
    public OtpRequiredException() {
        super("OTP verification required before signing");
    }
}
