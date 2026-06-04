package com.akitflow.signature.exception;

public class ConsentRequiredException extends SignatureException {
    public ConsentRequiredException() {
        super("Explicit consent is required to sign");
    }
}
