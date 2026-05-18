package com.akitflow.auth.exception;

public class OrganizationAlreadyExistsException extends RuntimeException {

    public OrganizationAlreadyExistsException(String name) {
        super("Bu organizasyon adı zaten kullanılıyor: " + name);
    }
}
