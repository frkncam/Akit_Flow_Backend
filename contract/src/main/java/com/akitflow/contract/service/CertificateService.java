package com.akitflow.contract.service;

import com.akitflow.contract.config.AppProperties;
import com.akitflow.contract.domain.OrganizationCertificate;
import com.akitflow.contract.repository.OrganizationCertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final OrganizationCertificateRepository repository;
    private final AppProperties props;

    public record CertKeyPair(X509Certificate certificate, PrivateKey privateKey) {}

    @Transactional
    public CertKeyPair getOrCreateCertificate(Long organizationId) {
        return repository.findByOrganizationId(organizationId)
                .map(this::fromEntity)
                .orElseGet(() -> createAndStore(organizationId));
    }

    private CertKeyPair createAndStore(Long organizationId) {
        try {
            AppProperties.Signature.Certificate cfg = props.signature().certificate();
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(cfg.keySize(), new SecureRandom());
            KeyPair keyPair = gen.generateKeyPair();

            X500Name subject = new X500Name("CN=AkitFlow Org " + organizationId + ", O=AkitFlow");
            BigInteger serial = new BigInteger(64, new SecureRandom());
            Instant now = Instant.now();
            Date notBefore = Date.from(now);
            Date notAfter = Date.from(now.plus(cfg.validityYears(), ChronoUnit.YEARS));

            X509CertificateHolder holder = new JcaX509v3CertificateBuilder(
                    subject, serial, notBefore, notAfter, subject, keyPair.getPublic()
            ).build(new JcaContentSignerBuilder(cfg.algorithm()).build(keyPair.getPrivate()));

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(
                    new java.io.ByteArrayInputStream(holder.getEncoded())
            );

            String certPem = toPem("CERTIFICATE", cert.getEncoded());
            String keyPem = toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded());

            OrganizationCertificate entity = OrganizationCertificate.builder()
                    .organizationId(organizationId)
                    .certificatePem(certPem)
                    .privateKeyPem(keyPem)
                    .serialNumber(serial.toString(16))
                    .validFrom(now)
                    .validUntil(Instant.from(notAfter.toInstant()))
                    .build();
            repository.save(entity);

            log.info("Self-signed certificate created for org {}", organizationId);
            return new CertKeyPair(cert, keyPair.getPrivate());
        } catch (Exception e) {
            throw new IllegalStateException("Sertifika oluşturulamadı: " + e.getMessage(), e);
        }
    }

    private CertKeyPair fromEntity(OrganizationCertificate entity) {
        try {
            byte[] certBytes = Base64.getMimeDecoder().decode(
                    entity.getCertificatePem()
                            .replace("-----BEGIN CERTIFICATE-----", "")
                            .replace("-----END CERTIFICATE-----", "")
            );
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(
                    new java.io.ByteArrayInputStream(certBytes)
            );

            byte[] keyBytes = Base64.getMimeDecoder().decode(
                    entity.getPrivateKeyPem()
                            .replace("-----BEGIN PRIVATE KEY-----", "")
                            .replace("-----END PRIVATE KEY-----", "")
            );
            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
            PrivateKey key = java.security.KeyFactory.getInstance("RSA").generatePrivate(spec);

            return new CertKeyPair(cert, key);
        } catch (Exception e) {
            throw new IllegalStateException("Sertifika yüklenemedi: " + e.getMessage(), e);
        }
    }

    private String toPem(String type, byte[] der) {
        StringWriter w = new StringWriter();
        w.write("-----BEGIN " + type + "-----\n");
        w.write(Base64.getMimeEncoder().encodeToString(der));
        w.write("\n-----END " + type + "-----\n");
        return w.toString();
    }
}
