package com.akitflow.signature.exception;

import com.akitflow.common.exception.BaseExceptionHandler;
import com.akitflow.common.web.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseExceptionHandler {

    @ExceptionHandler(SignatureExpiredException.class)
    public ResponseEntity<ErrorResponse> handleSignatureExpired(SignatureExpiredException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.GONE, ex.getMessage(), request);
    }

    @ExceptionHandler(PdfSigningFailedException.class)
    public ResponseEntity<ErrorResponse> handlePdfSigningFailed(PdfSigningFailedException ex,
                                                                 HttpServletRequest request) {
        log.error("PDF signing failed", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    @ExceptionHandler(CertificateLoadingException.class)
    public ResponseEntity<ErrorResponse> handleCertificateLoading(CertificateLoadingException ex,
                                                                   HttpServletRequest request) {
        log.error("Sertifika yüklenemedi", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz yok.", request);
    }

    @ExceptionHandler(ConsentRequiredException.class)
    public ResponseEntity<ErrorResponse> handleConsentRequired(ConsentRequiredException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(OtpRequiredException.class)
    public ResponseEntity<ErrorResponse> handleOtpRequired(OtpRequiredException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(OtpInvalidException.class)
    public ResponseEntity<ErrorResponse> handleOtpInvalid(OtpInvalidException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ErrorResponse> handleOtpExpired(OtpExpiredException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(OtpAttemptsExceededException.class)
    public ResponseEntity<ErrorResponse> handleOtpAttemptsExceeded(OtpAttemptsExceededException ex,
                                                                    HttpServletRequest request) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
    }

    @ExceptionHandler(OtpResendTooSoonException.class)
    public ResponseEntity<ErrorResponse> handleOtpResendTooSoon(OtpResendTooSoonException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
    }
}
