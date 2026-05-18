package com.akitflow.auth.controller;

import com.akitflow.auth.config.JwtConfig;
import com.akitflow.auth.config.JwtProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

/**
 * JSON Web Key Set endpoint. Other services (api-gateway, contract-service)
 * fetch the auth-service's public key from here to validate JWTs.
 *
 * Public — no authentication needed. The endpoint exposes only the public
 * portion of the keypair; the private key never leaves this service.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtProperties jwtProperties;

    @GetMapping("/oauth2/jwks")
    public Map<String, Object> jwks() {
        RSAPublicKey publicKey = JwtConfig.parsePublicKey(jwtProperties.publicKey());
        RSAKey jwk = new RSAKey.Builder(publicKey)
                .keyID("akitflow-auth-1")
                .keyUse(KeyUse.SIGNATURE)
                .build();
        return new JWKSet(jwk).toJSONObject();
    }
}
