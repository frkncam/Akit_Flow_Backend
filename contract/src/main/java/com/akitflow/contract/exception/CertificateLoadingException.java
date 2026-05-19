package com.akitflow.contract.exception;

public class CertificateLoadingException extends SignatureException {

    public CertificateLoadingException(String message) {
        super(message);
    }

    public CertificateLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
