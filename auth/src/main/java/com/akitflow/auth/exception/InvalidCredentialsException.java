package com.akitflow.auth.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email veya şifre hatalı.");
    }
}
