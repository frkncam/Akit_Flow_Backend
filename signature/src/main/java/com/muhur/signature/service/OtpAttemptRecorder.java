package com.muhur.signature.service;

import com.muhur.signature.repository.SignatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Başarısız OTP denemesini, çağıran transaction rollback olsa bile kalıcı kılmak için
 * ayrı (REQUIRES_NEW) bir transaction'da otp_attempts sayacını artırır. Aksi halde
 * OtpInvalidException dış transaction'ı geri sarar ve deneme sayacı asla artmaz
 * (brute-force koruması devre dışı kalır).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OtpAttemptRecorder {

    private final SignatureRepository signatureRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(Long signatureId) {
        signatureRepository.findByIdForUpdate(signatureId).ifPresent(s -> {
            int attempts = s.getOtpAttempts() != null ? s.getOtpAttempts() : 0;
            s.setOtpAttempts(attempts + 1);
            log.debug("OTP failed attempt recorded: signatureId={}, attempts={}",
                    signatureId, attempts + 1);
        });
    }
}
