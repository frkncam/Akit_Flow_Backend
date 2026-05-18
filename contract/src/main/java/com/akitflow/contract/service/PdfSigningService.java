package com.akitflow.contract.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;

@Service
@Slf4j
public class PdfSigningService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Istanbul"));

    public byte[] sign(byte[] pdfContent, X509Certificate cert, PrivateKey key,
                       String signerName, String reason) throws Exception {
        PDDocument doc = Loader.loadPDF(pdfContent);
        addSignatureAppearance(doc, signerName, reason);
        PDSignature pdSig = createPdSignature(doc, signerName, reason);

        SignatureInterface signer = content -> {
            try {
                return buildCmsSignedData(content.readAllBytes(), cert, key);
            } catch (Exception e) {
                throw new java.io.IOException("CMS imza oluşturulamadı", e);
            }
        };

        SignatureOptions opts = new SignatureOptions();
        doc.addSignature(pdSig, signer, opts);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        doc.save(result);
        doc.close();
        return result.toByteArray();
    }

    private void addSignatureAppearance(PDDocument doc, String signerName, String reason) throws Exception {
        PDPage page = doc.getPage(0);
        PDRectangle mediaBox = page.getMediaBox();
        float margin = 50;
        float boxW = 240;
        float boxH = 70;
        float x = mediaBox.getWidth() - boxW - margin;
        float y = margin;
        String now = DATE_FMT.format(Instant.now());

        try (PDPageContentStream cs = new PDPageContentStream(
                doc, page, PDPageContentStream.AppendMode.APPEND, true)) {

            cs.setStrokingColor(0.3f, 0.3f, 0.3f);
            cs.setLineWidth(1);
            cs.addRect(x, y, boxW, boxH);
            cs.stroke();

            PDType1Font f = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fb = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            cs.beginText();
            cs.setFont(fb, 8);
            cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
            cs.newLineAtOffset(x + 8, y + boxH - 14);
            cs.showText("DIGITAL OLARAK IMZALANMISTIR");
            cs.endText();

            cs.beginText();
            cs.setFont(f, 8);
            cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
            cs.newLineAtOffset(x + 8, y + boxH - 28);
            cs.showText("Imzaci: " + truncate(signerName, 40));
            cs.endText();

            cs.beginText();
            cs.setFont(f, 8);
            cs.newLineAtOffset(x + 8, y + boxH - 40);
            cs.showText("Sozlesme: " + truncate(reason, 40));
            cs.endText();

            cs.beginText();
            cs.setFont(f, 7);
            cs.setNonStrokingColor(0.4f, 0.4f, 0.4f);
            cs.newLineAtOffset(x + 8, y + boxH - 52);
            cs.showText("Tarih: " + now + " (TSI)");
            cs.endText();

            cs.beginText();
            cs.setFont(f, 6);
            cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
            cs.newLineAtOffset(x + 8, y + 8);
            cs.showText("AkitFlow e-Imza — 5070 sayili kanun kapsaminda degildir");
            cs.endText();
        }
    }

    private PDSignature createPdSignature(PDDocument doc, String signerName, String reason) throws java.io.IOException {
        PDSignature sig = new PDSignature();
        sig.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        sig.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        sig.setName(signerName);
        sig.setReason(reason);
        sig.setSignDate(Calendar.getInstance());
        doc.addSignature(sig);
        return sig;
    }

    private byte[] buildCmsSignedData(byte[] content, X509Certificate cert, PrivateKey key)
            throws Exception {
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

        JcaContentSignerBuilder signerBuilder =
                new JcaContentSignerBuilder("SHA256withRSA");
        gen.addSignerInfoGenerator(
                new org.bouncycastle.cms.SignerInfoGeneratorBuilder(
                        new org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder()
                                .setProvider("BC").build())
                        .build(signerBuilder.build(key),
                                new JcaX509CertificateHolder(cert)));

        gen.addCertificates(new JcaCertStore(List.of(
                new JcaX509CertificateHolder(cert))));

        CMSSignedData signedData = gen.generate(
                new CMSProcessableByteArray(content), false);

        return signedData.getEncoded();
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }
}
