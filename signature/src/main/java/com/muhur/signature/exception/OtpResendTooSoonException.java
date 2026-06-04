package com.muhur.signature.exception;

public class OtpResendTooSoonException extends SignatureException {
    public OtpResendTooSoonException() {
        super("OTP was requested too recently; please wait before requesting a new code");
    }
}
