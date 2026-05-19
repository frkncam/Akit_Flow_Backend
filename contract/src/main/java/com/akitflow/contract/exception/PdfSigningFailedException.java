package com.akitflow.contract.exception;

public class PdfSigningFailedException extends SignatureException {

    public PdfSigningFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
