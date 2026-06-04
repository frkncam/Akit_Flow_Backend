package com.akitflow.signature.service;

import com.akitflow.signature.config.AppProperties;
import com.akitflow.signature.domain.Signature;
import com.akitflow.signature.exception.*;
import com.akitflow.signature.util.DocumentHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    /** nextInt(bound) int sınırını aşmamak için desteklenen azami kod uzunluğu. */
    private static final int MAX_CODE_LENGTH = 9;

    private final AppProperties appProperties;
    private final OtpAttemptRecorder attemptRecorder;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Aynı OTP'nin çok sık yeniden üretilmesini (e-posta bombardımanı + sayaç sıfırlama
     * suistimali) engeller. Son üretimden bu yana resendIntervalSeconds geçmediyse reddeder.
     */
    public void ensureResendAllowed(Signature sig) {
        AppProperties.Signature.Otp otp = appProperties.signature().otp();
        if (sig.getOtpExpiresAt() == null) {
            return; // hiç üretilmemiş
        }
        Instant issuedAt = sig.getOtpExpiresAt().minusSeconds(otp.ttlMinutes() * 60L);
        Instant nextAllowed = issuedAt.plusSeconds(otp.resendIntervalSeconds());
        if (nextAllowed.isAfter(Instant.now())) {
            throw new OtpResendTooSoonException();
        }
    }

    public String generateAndStore(Signature sig) {
        AppProperties.Signature.Otp otp = appProperties.signature().otp();
        int len = Math.min(otp.length(), MAX_CODE_LENGTH);
        int bound = (int) Math.pow(10, len);
        String code = String.format("%0" + len + "d", secureRandom.nextInt(bound));

        String hash = DocumentHasher.sha256Hex((sig.getToken() + ":" + code).getBytes());
        sig.setOtpHash(hash);
        sig.setOtpExpiresAt(Instant.now().plusSeconds(otp.ttlMinutes() * 60L));
        sig.setOtpAttempts(0);
        log.debug("OTP generated for signature id={}, ttl={}min", sig.getId(), otp.ttlMinutes());
        return code;
    }

    public void verify(Signature sig, String code) {
        AppProperties.Signature.Otp otp = appProperties.signature().otp();

        if (sig.getOtpVerifiedAt() != null) {
            return;
        }

        if (sig.getOtpHash() == null || sig.getOtpExpiresAt() == null
                || sig.getOtpExpiresAt().isBefore(Instant.now())) {
            throw new OtpExpiredException();
        }

        int attempts = sig.getOtpAttempts() != null ? sig.getOtpAttempts() : 0;
        if (attempts >= otp.maxAttempts()) {
            throw new OtpAttemptsExceededException();
        }

        String expectedHash = DocumentHasher.sha256Hex((sig.getToken() + ":" + code).getBytes());
        if (!expectedHash.equals(sig.getOtpHash())) {
            // Denemeyi, dış transaction rollback olsa bile kalıcı kıl (REQUIRES_NEW).
            attemptRecorder.recordFailedAttempt(sig.getId());
            sig.setOtpAttempts(attempts + 1); // bellekteki kopyayı da tutarlı tut
            throw new OtpInvalidException();
        }

        sig.setOtpVerifiedAt(Instant.now());
    }

    public void requireFreshlyVerified(Signature sig) {
        AppProperties.Signature.Otp otp = appProperties.signature().otp();
        if (sig.getOtpVerifiedAt() == null
                || sig.getOtpVerifiedAt().isBefore(
                        Instant.now().minusSeconds(otp.verificationWindowMinutes() * 60L))) {
            throw new OtpRequiredException();
        }
    }
}
