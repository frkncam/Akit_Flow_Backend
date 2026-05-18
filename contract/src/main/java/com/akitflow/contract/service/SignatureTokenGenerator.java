package com.akitflow.contract.service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates URL-safe random tokens for signature links. Same pattern as
 * auth-service refresh token generation: 256-bit SecureRandom, Base64-URL
 * encoded without padding.
 */
public final class SignatureTokenGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SignatureTokenGenerator() {}

    public static String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
