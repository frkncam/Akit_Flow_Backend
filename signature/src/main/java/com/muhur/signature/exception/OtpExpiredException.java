package com.muhur.signature.exception;

public class OtpExpiredException extends SignatureException {
    public OtpExpiredException() {
        super("OTP code has expired");
    }
}
