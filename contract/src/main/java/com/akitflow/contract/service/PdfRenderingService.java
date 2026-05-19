package com.akitflow.contract.service;

import java.util.Map;

public interface PdfRenderingService {

    String renderHtml(String thymeleafSource, Map<String, Object> model);

    byte[] htmlToPdf(String html);
}
