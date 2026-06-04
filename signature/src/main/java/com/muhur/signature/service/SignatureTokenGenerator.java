package com.muhur.signature.service;

import java.security.SecureRandom;
import java.util.Base64;

public final class SignatureTokenGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SignatureTokenGenerator() {}

    public static String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
