package com.akitflow.auth.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Bu email adresi zaten kayıtlı: " + email);
    }
}
