package com.akitflow.contract.service;

import com.akitflow.contract.config.AppProperties;
import com.akitflow.contract.domain.OrganizationCertificate;
import com.akitflow.contract.exception.CertificateLoadingException;
import com.akitflow.contract.repository.OrganizationCertificateRepository;
import com.akitflow.contract.service.util.PemUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateServiceImpl implements CertificateService {

    private final OrganizationCertificateRepository repository;
    private final AppProperties props;

    @Override
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

            String certPem = PemUtils.writeCertificate(cert);
            String keyPem = PemUtils.writePrivateKey(keyPair.getPrivate());

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
        } catch (CertificateLoadingException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateLoadingException("Sertifika oluşturulamadı: " + e.getMessage(), e);
        }
    }

    private CertKeyPair fromEntity(OrganizationCertificate entity) {
        X509Certificate cert = PemUtils.parseCertificate(entity.getCertificatePem());
        PrivateKey key = PemUtils.parsePrivateKey(entity.getPrivateKeyPem());
        return new CertKeyPair(cert, key);
    }
}
