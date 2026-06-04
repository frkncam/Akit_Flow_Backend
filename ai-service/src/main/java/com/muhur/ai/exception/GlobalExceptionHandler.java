package com.muhur.ai.exception;

import com.muhur.common.exception.BaseExceptionHandler;
import com.muhur.common.web.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseExceptionHandler {

    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAiUnavailable(AiServiceUnavailableException ex,
                                                              HttpServletRequest request) {
        log.error("AI servisi kullanilamiyor: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(GenerationValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationFailed(GenerationValidationException ex,
                                                                  HttpServletRequest request) {
        log.warn("Sablon validasyon hatasi: {}", ex.getDetails());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(
                        java.time.Instant.now(),
                        422,
                        "Generation Validation Failed",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex,
                                                          HttpServletRequest request) {
        log.warn("Rate limit asildi: {}", ex.getMessage());
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
    }
}
