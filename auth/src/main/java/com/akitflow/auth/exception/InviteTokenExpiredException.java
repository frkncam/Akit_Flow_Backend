package com.akitflow.auth.exception;

public class InviteTokenExpiredException extends RuntimeException {

    public InviteTokenExpiredException(String message) {
        super(message);
    }
}
