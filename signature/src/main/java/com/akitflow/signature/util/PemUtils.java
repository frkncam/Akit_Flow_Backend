package com.akitflow.signature.util;

import com.akitflow.signature.exception.CertificateLoadingException;

import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public final class PemUtils {

    private PemUtils() {}

    private static final String CERT_BEGIN = "-----BEGIN CERTIFICATE-----";
    private static final String CERT_END   = "-----END CERTIFICATE-----";
    private static final String KEY_BEGIN  = "-----BEGIN PRIVATE KEY-----";
    private static final String KEY_END    = "-----END PRIVATE KEY-----";

    public static X509Certificate parseCertificate(String pem) {
        try {
            String b64 = stripPemMarkers(pem, CERT_BEGIN, CERT_END);
            byte[] der = Base64.getMimeDecoder().decode(b64);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(
                    new java.io.ByteArrayInputStream(der));
        } catch (Exception e) {
            throw new CertificateLoadingException("Sertifika parse edilemedi", e);
        }
    }

    public static PrivateKey parsePrivateKey(String pem) {
        try {
            String b64 = stripPemMarkers(pem, KEY_BEGIN, KEY_END);
            byte[] der = Base64.getMimeDecoder().decode(b64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new CertificateLoadingException("Private key parse edilemedi", e);
        }
    }

    public static String writeCertificate(X509Certificate cert) {
        try {
            return writePem("CERTIFICATE", cert.getEncoded());
        } catch (Exception e) {
            throw new CertificateLoadingException("Sertifika encode edilemedi", e);
        }
    }

    public static String writePrivateKey(PrivateKey key) {
        return writePem("PRIVATE KEY", key.getEncoded());
    }

    private static String writePem(String type, byte[] der) {
        StringWriter w = new StringWriter();
        w.write("-----BEGIN " + type + "-----\n");
        w.write(Base64.getMimeEncoder().encodeToString(der));
        w.write("\n-----END " + type + "-----\n");
        return w.toString();
    }

    private static String stripPemMarkers(String pem, String begin, String end) {
        return pem
                .replace(begin, "")
                .replace(end, "")
                .replace("\r", "")
                .replace("\n", "")
                .strip();
    }
}
