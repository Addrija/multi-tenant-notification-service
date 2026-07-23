package com.notification.service.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    void substitutesAllPlaceholders() {
        String result = renderer.render(
                "Hi {{name}}, your order {{orderId}} has shipped!",
                Map.of("name", "Jane", "orderId", "ORD-123"));

        assertThat(result).isEqualTo("Hi Jane, your order ORD-123 has shipped!");
    }

    @Test
    void returnsTemplateUnchangedWhenNoPlaceholders() {
        String result = renderer.render("No variables here.", Map.of());

        assertThat(result).isEqualTo("No variables here.");
    }

    @Test
    void substitutesRepeatedPlaceholder() {
        String result = renderer.render("{{name}} {{name}}", Map.of("name", "Jane"));

        assertThat(result).isEqualTo("Jane Jane");
    }

    @Test
    void throwsOnMissingVariable() {
        assertThatThrownBy(() -> renderer.render("Hi {{name}}", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void throwsOnNullVariablesMapWithPlaceholder() {
        assertThatThrownBy(() -> renderer.render("Hi {{name}}", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}