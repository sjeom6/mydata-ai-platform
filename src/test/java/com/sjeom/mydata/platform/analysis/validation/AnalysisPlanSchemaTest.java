package com.sjeom.mydata.platform.analysis.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class AnalysisPlanSchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void schemaIsValidJsonAndRejectsUnknownProperties() throws IOException {
        try (var input = getClass().getResourceAsStream("/schema/analysis-plan.schema.json")) {
            assertThat(input).isNotNull();
            JsonNode schema = objectMapper.readTree(input);

            assertThat(schema.path("$schema").asText())
                    .isEqualTo("https://json-schema.org/draft/2020-12/schema");
            assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
            assertThat(schema.path("properties").path("period")
                    .path("properties").path("value").path("maximum").asInt()).isEqualTo(12);
            assertThat(schema.path("properties").path("toolSteps")
                    .path("prefixItems").size()).isEqualTo(5);
        }
    }
}
