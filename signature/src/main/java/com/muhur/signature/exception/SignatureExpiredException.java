package com.muhur.signature.exception;

public class SignatureExpiredException extends RuntimeException {

    public SignatureExpiredException() {
        super("Signature link expired.");
    }
}
