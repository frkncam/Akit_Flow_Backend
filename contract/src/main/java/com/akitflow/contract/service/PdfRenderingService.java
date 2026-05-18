package com.akitflow.contract.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Map;

/**
 * Renders Thymeleaf template strings into HTML and converts HTML to PDF
 * using openhtmltopdf. Templates are stored in the DB; binary PDFs are
 * uploaded to MinIO by the calling service.
 */
@Service
@RequiredArgsConstructor
public class PdfRenderingService {

    @Qualifier("stringTemplateEngine")
    private final SpringTemplateEngine stringTemplateEngine;

    public String renderHtml(String thymeleafSource, Map<String, Object> model) {
        Context ctx = new Context(Locale.forLanguageTag("tr-TR"));
        ctx.setVariables(model);
        return stringTemplateEngine.process(thymeleafSource, ctx);
    }

    public byte[] htmlToPdf(String html) {
        String xhtml = toXhtml(html);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(xhtml, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("PDF üretilemedi: " + e.getMessage(), e);
        }
    }

    /**
     * openhtmltopdf requires strict XHTML — void elements like &lt;meta&gt;,
     * &lt;br&gt;, &lt;img&gt; must be self-closing. We let jsoup parse the
     * user's template output (which may be lenient HTML) and serialise it
     * back as XML-syntax XHTML so the user does not have to remember XHTML
     * rules when writing templates.
     */
    private String toXhtml(String html) {
        Document doc = Jsoup.parse(html);
        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset("UTF-8");
        return doc.html();
    }
}
