package com.muhur.signature.exception;

public class OtpAttemptsExceededException extends SignatureException {
    public OtpAttemptsExceededException() {
        super("Maximum OTP attempts exceeded");
    }
}
