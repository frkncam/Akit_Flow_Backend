package com.akitflow.signature.service;

import com.akitflow.signature.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.tsp.*;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimestampService {

    private final AppProperties appProperties;
    private final SecureRandom nonceGenerator = new SecureRandom();

    public Optional<TimeStampToken> timestamp(byte[] signatureBytes) {
        AppProperties.Signature.Tsa tsa = appProperties.signature().tsa();
        if (!tsa.enabled()) {
            log.debug("TSA disabled, skipping timestamp");
            return Optional.empty();
        }

        try {
            TimeStampRequestGenerator g = new TimeStampRequestGenerator();
            g.setCertReq(true);

            byte[] digest = MessageDigest.getInstance("SHA-256").digest(signatureBytes);
            BigInteger nonce = new BigInteger(64, nonceGenerator);
            TimeStampRequest req = g.generate(TSPAlgorithms.SHA256, digest, nonce);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(tsa.timeoutSeconds()))
                    .build();

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(tsa.url()))
                    .timeout(Duration.ofSeconds(tsa.timeoutSeconds()))
                    .header("Content-Type", "application/timestamp-query")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(req.getEncoded()))
                    .build();

            HttpResponse<byte[]> httpResp = client.send(httpReq, HttpResponse.BodyHandlers.ofByteArray());

            if (httpResp.statusCode() != 200) {
                log.warn("TSA returned HTTP {} from {}", httpResp.statusCode(), tsa.url());
                return Optional.empty();
            }

            TimeStampResponse tsResp = new TimeStampResponse(httpResp.body());
            tsResp.validate(req);

            TimeStampToken token = tsResp.getTimeStampToken();
            log.info("TSA timestamp obtained: time={}, authority={}",
                    token.getTimeStampInfo().getGenTime(),
                    token.getTimeStampInfo().getTsa());
            return Optional.of(token);

        } catch (HttpTimeoutException e) {
            log.warn("TSA request timed out ({}s): {}", tsa.timeoutSeconds(), tsa.url());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("TSA timestamp failed (graceful degradation): {}", e.getMessage());
            return Optional.empty();
        }
    }
}
