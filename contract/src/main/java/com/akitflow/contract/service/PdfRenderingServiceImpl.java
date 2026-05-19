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

@Service
@RequiredArgsConstructor
public class PdfRenderingServiceImpl implements PdfRenderingService {

    @Qualifier("stringTemplateEngine")
    private final SpringTemplateEngine stringTemplateEngine;

    @Override
    public String renderHtml(String thymeleafSource, Map<String, Object> model) {
        Context ctx = new Context(Locale.forLanguageTag("tr-TR"));
        ctx.setVariables(model);
        return stringTemplateEngine.process(thymeleafSource, ctx);
    }

    @Override
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

    private String toXhtml(String html) {
        Document doc = Jsoup.parse(html);
        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset("UTF-8");
        return doc.html();
    }
}
