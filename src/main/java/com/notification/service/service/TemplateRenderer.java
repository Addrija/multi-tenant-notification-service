package com.notification.service.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Renders {{var}} placeholders in a template body using a supplied variables map.
// Fails fast (rather than silently leaving the placeholder in the output) if a
// placeholder has no matching variable - cheap validation win at submission time.
@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    public String render(String templateBody, Map<String, String> variables) {
        Map<String, String> vars = variables != null ? variables : Map.of();
        Matcher matcher = PLACEHOLDER.matcher(templateBody);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);
            if (!vars.containsKey(key)) {
                throw new IllegalArgumentException("Missing value for template variable: " + key);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(vars.get(key)));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}