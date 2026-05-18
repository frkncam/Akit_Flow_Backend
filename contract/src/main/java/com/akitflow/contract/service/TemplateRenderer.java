package com.akitflow.contract.service;

import com.akitflow.contract.domain.TemplateVariableData;
import com.akitflow.contract.domain.TemplateVariableSource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces {{key}} placeholders in a stored template's bodyHtml with values
 * resolved from the template's variable metadata, a system-binding lookup,
 * and an optional caller-supplied custom values map. Matches the syntax used
 * by the frontend RichTextEditor and its client-side preview fallback.
 */
@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-z][a-z0-9_]*)\\s*\\}\\}");

    /**
     * Build the resolved value map: per template variable choose a value from
     * (1) caller-supplied customValues, (2) systemBindings lookup when
     * source=SYSTEM, (3) the variable's defaultValue.
     *
     * @param variables       the template's declared variables
     * @param systemBindings  map of system-binding key (e.g. "contract.title")
     *                        to its resolved string representation; may be empty
     * @param customValues    user-provided overrides keyed by variable.key
     */
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

    /**
     * Substitute {{key}} occurrences in source with resolved values. Unknown
     * keys are left as the original placeholder so the caller / reader can
     * easily spot them.
     */
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
