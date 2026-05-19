package com.akitflow.signature.service;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public interface PdfSigningService {

    byte[] sign(byte[] pdfContent, X509Certificate cert, PrivateKey key,
                String signerName, String reason) throws Exception;
}
