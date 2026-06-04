package com.akitflow.signature.exception;

public class OtpInvalidException extends SignatureException {
    public OtpInvalidException() {
        super("Invalid OTP code");
    }
}
