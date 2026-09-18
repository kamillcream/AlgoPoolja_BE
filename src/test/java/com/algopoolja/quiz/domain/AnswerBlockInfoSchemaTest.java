package com.algopoolja.quiz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

class AnswerBlockInfoSchemaTest {

    @Test
    void structuredOutputSchemaRequiresEveryDeclaredProperty() {
        String schema = JsonSchemaGenerator.generateForType(AnswerBlockInfo.class);

        assertThat(schema)
                .contains("\"correctAnswers\"")
                .contains("\"wrongAnswers\"");
        assertThat(requiredProperties(schema))
                .containsExactlyInAnyOrder("correctAnswers", "wrongAnswers");
    }

    private static java.util.List<String> requiredProperties(String schema) {
        try {
            var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
            var required = mapper.readTree(schema).get("required");
            java.util.List<String> names = new java.util.ArrayList<>();
            required.forEach(node -> names.add(node.asText()));
            return names;
        }
        catch (tools.jackson.core.JacksonException exception) {
            throw new AssertionError("Generated schema must be valid JSON", exception);
        }
    }
}
