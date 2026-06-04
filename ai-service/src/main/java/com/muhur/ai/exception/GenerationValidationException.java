package com.muhur.ai.exception;

import java.util.List;

public class GenerationValidationException extends RuntimeException {

    private final List<String> details;

    public GenerationValidationException(List<String> details) {
        super("Sablon validasyon hatasi: " + String.join("; ", details));
        this.details = details;
    }

    public List<String> getDetails() {
        return details;
    }
}
