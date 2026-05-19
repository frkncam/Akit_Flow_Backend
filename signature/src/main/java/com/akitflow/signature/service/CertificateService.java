package com.akitflow.signature.service;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public interface CertificateService {

    record CertKeyPair(X509Certificate certificate, PrivateKey privateKey) {}

    CertKeyPair getOrCreateCertificate(Long organizationId);
}
