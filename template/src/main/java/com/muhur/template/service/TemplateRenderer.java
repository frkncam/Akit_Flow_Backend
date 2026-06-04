package com.muhur.template.service;

import com.muhur.template.domain.TemplateVariableData;
import com.muhur.template.domain.TemplateVariableSource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-z][a-z0-9_]*)\\s*\\}\\}");

    public Map<String, String> resolveValues(List<TemplateVariableData> variables,
                                             Map<String, String> systemBindings,
                                             Map<String, String> customValues) {
        Map<String, String> resolved = new HashMap<>();
        if (variables == null) return resolved;
        for (TemplateVariableData v : variables) {
            String value = null;
            if (customValues != null && customValues.containsKey(v.key())) {
                value = customValues.get(v.key());
            } else if (v.source() == TemplateVariableSource.SYSTEM
                    && v.systemBinding() != null
                    && systemBindings.containsKey(v.systemBinding())) {
                value = systemBindings.get(v.systemBinding());
            } else if (v.defaultValue() != null) {
                value = v.defaultValue();
            }
            if (value != null) {
                resolved.put(v.key(), value);
            }
        }
        return resolved;
    }

    public String substitute(String source, Map<String, String> values) {
        if (source == null || source.isEmpty()) return source;
        Matcher m = PLACEHOLDER.matcher(source);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String replacement = values.get(key);
            m.appendReplacement(out, Matcher.quoteReplacement(replacement != null ? replacement : m.group(0)));
        }
        m.appendTail(out);
        return out.toString();
    }
}
