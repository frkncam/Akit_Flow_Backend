package com.akitflow.signature.service;

import com.akitflow.signature.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
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
import java.io.File;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfSigningServiceImpl implements PdfSigningService {

    private final AppProperties props;

    @Override
    public byte[] sign(byte[] pdfContent, X509Certificate cert, PrivateKey key,
                       String signerName, String reason) throws Exception {
        PDDocument doc = Loader.loadPDF(pdfContent);
        addSignatureAppearance(doc, signerName, reason);
        PDSignature pdSig = createPdSignature(signerName, reason);

        SignatureInterface signer = content -> {
            try {
                return buildCmsSignedData(content.readAllBytes(), cert, key);
            } catch (Exception e) {
                throw new java.io.IOException("CMS signature creation failed", e);
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
        AppProperties.Signature.Appearance a = props.signature().appearance();

        PDPage page = doc.getPage(0);
        PDRectangle mediaBox = page.getMediaBox();

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.of(a.timezone()));

        float margin = a.margin();
        float boxW = a.boxWidth();
        float boxH = a.boxHeight();
        float x = mediaBox.getWidth() - boxW - margin;
        float y = margin;
        String now = dateFmt.format(Instant.now());
        String tsLabel = "Europe/Istanbul".equals(a.timezone()) ? " (TSI)" : "";

        PDType0Font unicodeFont = loadUnicodeFont(doc);
        boolean useUnicode = unicodeFont != null;

        try (PDPageContentStream cs = new PDPageContentStream(
                doc, page, PDPageContentStream.AppendMode.APPEND, true)) {

            cs.setStrokingColor(0.3f, 0.3f, 0.3f);
            cs.setLineWidth(1);
            cs.addRect(x, y, boxW, boxH);
            cs.stroke();

            PDType1Font fb = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            int fs = a.fontSize();

            cs.beginText();
            cs.setFont(fb, fs);
            cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
            cs.newLineAtOffset(x + 8, y + boxH - 14);
            cs.showText(useUnicode ? a.labelTr() : toAscii(a.labelTr()));
            cs.endText();

            String nameText = "Imzaci: " + truncate(signerName, 40);
            String reasonText = "Contract: " + truncate(reason, 40);

            cs.beginText();
            cs.setFont(useUnicode ? unicodeFont : fb, fs);
            cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
            cs.newLineAtOffset(x + 8, y + boxH - 28);
            cs.showText(useUnicode ? nameText : toAscii(nameText));
            cs.endText();

            cs.beginText();
            cs.setFont(useUnicode ? unicodeFont : fb, fs);
            cs.newLineAtOffset(x + 8, y + boxH - 40);
            cs.showText(useUnicode ? reasonText : toAscii(reasonText));
            cs.endText();

            int smallFs = Math.max(fs - 1, 5);
            cs.beginText();
            cs.setFont(useUnicode ? unicodeFont : fb, smallFs);
            cs.setNonStrokingColor(0.4f, 0.4f, 0.4f);
            cs.newLineAtOffset(x + 8, y + boxH - 52);
            cs.showText("Tarih: " + now + tsLabel);
            cs.endText();

            cs.beginText();
            cs.setFont(useUnicode ? unicodeFont : fb, Math.max(fs - 2, 4));
            cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
            cs.newLineAtOffset(x + 8, y + 8);
            String disclaimer = "AkitFlow e-Signature — not under Law No. 5070";
            cs.showText(useUnicode ? disclaimer : toAscii(disclaimer));
            cs.endText();
        }
    }

    private PDSignature createPdSignature(String signerName, String reason) {
        PDSignature sig = new PDSignature();
        sig.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        sig.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        sig.setName(signerName);
        sig.setReason(reason);
        sig.setSignDate(Calendar.getInstance());
        return sig;
    }

    private byte[] buildCmsSignedData(byte[] content, X509Certificate cert, PrivateKey key)
            throws Exception {
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

        JcaContentSignerBuilder signerBuilder =
                new JcaContentSignerBuilder(props.signature().certificate().algorithm());
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

    private PDType0Font loadUnicodeFont(PDDocument doc) {
        for (String path : new String[]{
                "C:\\Windows\\Fonts\\arial.ttf",
                "C:\\Windows\\Fonts\\calibri.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                "/System/Library/Fonts/Supplemental/Arial.ttf"
        }) {
            File f = new File(path);
            if (f.exists()) {
                try {
                    return PDType0Font.load(doc, f);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private String toAscii(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            sb.append(switch (s.charAt(i)) {
                case 'ş', 'Ş' -> 's';
                case 'ğ', 'Ğ' -> 'g';
                case 'İ', 'ı' -> 'i';
                case 'ç', 'Ç' -> 'c';
                case 'ü', 'Ü' -> 'u';
                case 'ö', 'Ö' -> 'o';
                case '—' -> '-';
                default -> s.charAt(i);
            });
        }
        return sb.toString();
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }
}
