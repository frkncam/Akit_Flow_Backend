package com.akitflow.auth.exception;

public class OrganizationAlreadyExistsException extends RuntimeException {

    public OrganizationAlreadyExistsException(String name) {
        super("Organization name already taken: " + name);
    }
}
