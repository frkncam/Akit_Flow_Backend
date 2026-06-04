package com.muhur.signature.service;

import com.muhur.signature.config.AppProperties;
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
import org.bouncycastle.cms.*;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tsp.TimeStampToken;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfSigningServiceImpl implements PdfSigningService {

    private final AppProperties props;
    private final TimestampService timestampService;

    @Override
    public SignResult sign(byte[] pdfContent, X509Certificate cert, PrivateKey key,
                           SignContext ctx) throws Exception {
        PDDocument doc = Loader.loadPDF(pdfContent);

        addSignatureAppearance(doc, ctx.signerName(), ctx.reason());
        appendAuditPage(doc, ctx);

        PDSignature pdSig = createPdSignature(ctx.signerName(), ctx.reason());

        // TSA zaman/otorite bilgisi imza lambda'sı içinde belirlenir; race oluşmaması için
        // singleton state yerine bu çağrıya özel local tutuculara yazılır (C2).
        final Instant[] tsaTime = new Instant[1];
        final String[] tsaAuthority = new String[1];

        SignatureInterface signer = content -> {
            try {
                byte[] cmsBytes = buildCmsSignedData(content.readAllBytes(), cert, key);
                return attachTsaIfAvailable(cmsBytes, tsaTime, tsaAuthority);
            } catch (Exception e) {
                throw new java.io.IOException("CMS signature creation failed", e);
            }
        };

        SignatureOptions opts = new SignatureOptions();
        doc.addSignature(pdSig, signer, opts);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        doc.save(result);
        doc.close();

        return new SignResult(result.toByteArray(), tsaTime[0], tsaAuthority[0]);
    }

    private byte[] attachTsaIfAvailable(byte[] cmsBytes, Instant[] tsaTimeOut, String[] tsaAuthorityOut) {
        Optional<TimeStampToken> tsaToken = timestampService.timestamp(cmsBytes);
        if (tsaToken.isEmpty()) {
            return cmsBytes;
        }

        try {
            TimeStampToken token = tsaToken.get();
            CMSSignedData signedData = new CMSSignedData(cmsBytes);
            SignerInformationStore signerStore = signedData.getSignerInfos();
            SignerInformation signer = signerStore.getSigners().iterator().next();

            org.bouncycastle.asn1.ASN1EncodableVector v =
                    new org.bouncycastle.asn1.ASN1EncodableVector();
            org.bouncycastle.asn1.cms.AttributeTable unsigned = signer.getUnsignedAttributes();
            if (unsigned != null) {
                for (Object entry : unsigned.toHashtable().entrySet()) {
                    java.util.Map.Entry<?, ?> e = (java.util.Map.Entry<?, ?>) entry;
                    v.add((org.bouncycastle.asn1.ASN1Encodable) e.getValue());
                }
            }
            v.add(new org.bouncycastle.asn1.cms.Attribute(
                    org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.id_aa_signatureTimeStampToken,
                    new org.bouncycastle.asn1.DERSet(token.toCMSSignedData().toASN1Structure())));

            SignerInformation newSigner = SignerInformation.replaceUnsignedAttributes(
                    signer, new org.bouncycastle.asn1.cms.AttributeTable(v));

            CMSSignedData newSignedData = CMSSignedData.replaceSigners(signedData,
                    new SignerInformationStore(List.of(newSigner)));
            byte[] encoded = newSignedData.getEncoded();

            // TSA bilgisi yalnızca damga gerçekten CMS'e gömüldüyse raporlanır.
            tsaTimeOut[0] = token.getTimeStampInfo().getGenTime().toInstant();
            tsaAuthorityOut[0] = token.getTimeStampInfo().getTsa() != null
                    ? token.getTimeStampInfo().getTsa().toString()
                    : props.signature().tsa().url();
            return encoded;
        } catch (Exception e) {
            log.warn("TSA token could not be attached to CMS, falling back to plain signature", e);
            return cmsBytes;
        }
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
            String disclaimer = "Mühür e-Signature — not under Law No. 5070";
            cs.showText(useUnicode ? disclaimer : toAscii(disclaimer));
            cs.endText();
        }
    }

    private void appendAuditPage(PDDocument doc, SignContext ctx) throws Exception {
        PDPage auditPage = new PDPage(PDRectangle.A4);
        doc.addPage(auditPage);

        PDType0Font unicodeFont = loadUnicodeFont(doc);
        PDType1Font fb = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font fbBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        boolean useUnicode = unicodeFont != null;

        try (PDPageContentStream cs = new PDPageContentStream(
                doc, auditPage, PDPageContentStream.AppendMode.OVERWRITE, true)) {

            float margin = 50;
            float y = PDRectangle.A4.getHeight() - margin;
            float lineHeight = 16;
            float fontSize = 10;
            float smallFont = 8;

            cs.setNonStrokingColor(0.1f, 0.1f, 0.1f);

            cs.beginText();
            cs.setFont(fbBold, 14);
            cs.newLineAtOffset(margin, y);
            cs.showText("IMZA DOGRULAMA / SIGNATURE AUDIT");
            cs.endText();
            y -= 30;

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss 'UTC'")
                    .withZone(ZoneId.of("UTC"));
            String nowStr = dateFmt.format(Instant.now());

            // Audit sayfası imza üretiminden ÖNCE çizilir; bu yüzden kesin TSA zamanı
            // burada henüz bilinmez. Gerçek/otoriter zaman damgası CMS'e gömülür ve
            // signature_metadata içinde saklanır. Burada yalnızca yapılandırılmış TSA
            // otoritesi ve imza-anı (sunucu) zamanı dürüstçe gösterilir.
            AppProperties.Signature.Tsa tsa = props.signature().tsa();
            String tsaDisplay = tsa.enabled()
                    ? tsa.url() + " (CMS'e gomulu / embedded)"
                    : "Yok / None";

            String[][] rows = {
                    {"Imzaci / Signer:", ctx.signerName() + " <" + ctx.signerEmail() + ">"},
                    {"Imza Zamani / Signing Time:", nowStr},
                    {"Belge SHA-256 / Document Hash:", ctx.documentHash()},
                    {"OTP Dogrulandi / OTP Verified:", ctx.otpVerified() ? "Evet / Yes" : "Hayir / No"},
                    {"Imzaci IP / Signer IP:", maskIp(ctx.signerIp())},
                    {"Sertifika Seri No / Cert Serial:", ctx.certificateSerial()},
                    {"Algoritma / Algorithm:", ctx.algorithm()},
                    {"TSA / Timestamp Authority:", tsaDisplay},
                    {"Riza / Consent:", truncate(ctx.consentText(), 100)},
            };

            for (String[] row : rows) {
                y -= lineHeight + 2;

                cs.beginText();
                cs.setFont(fbBold, fontSize);
                cs.newLineAtOffset(margin, y);
                cs.showText(row[0]);
                cs.endText();

                cs.beginText();
                cs.setFont(useUnicode ? unicodeFont : fb, fontSize);
                cs.newLineAtOffset(margin + 180, y);
                cs.showText(useUnicode ? row[1] : toAscii(row[1]));
                cs.endText();
            }

            y -= 30;

            cs.setStrokingColor(0.8f, 0.8f, 0.8f);
            cs.setLineWidth(0.5f);
            cs.moveTo(margin, y);
            cs.lineTo(PDRectangle.A4.getWidth() - margin, y);
            cs.stroke();
            y -= lineHeight + 4;

            cs.beginText();
            cs.setFont(useUnicode ? unicodeFont : fb, smallFont);
            cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
            cs.newLineAtOffset(margin, y);
            cs.showText("Mühür e-Signature — not under Law No. 5070 — Gelişmiş Elektronik İmza");
            cs.endText();

            y -= lineHeight;
            cs.beginText();
            cs.setFont(useUnicode ? unicodeFont : fb, smallFont);
            cs.newLineAtOffset(margin, y);
            cs.showText("Bu sayfa imza aninda olusturulmustur ve delil niteligi tasir. / This audit page is generated at signing time.");
            cs.endText();
        }
    }

    private String maskIp(String ip) {
        if (ip == null || ip.isBlank()) return "bilinmiyor / unknown";
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".xxx";
        }
        return ip;
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
