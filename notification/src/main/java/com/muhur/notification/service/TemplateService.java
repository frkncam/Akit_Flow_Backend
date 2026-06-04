package com.muhur.notification.service;

import java.util.Map;

public interface TemplateService {

    String render(String templateName, Map<String, Object> variables);
}
