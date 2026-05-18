package com.akitflow.contract.exception;

public class SignatureExpiredException extends RuntimeException {

    public SignatureExpiredException() {
        super("İmza linki süresi dolmuş.");
    }
}
