package com.sjeom.mydata.platform.analysis.input;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.sjeom.mydata.platform.analysis.domain.AnalysisPlan;
import com.sjeom.mydata.platform.analysis.validation.AnalysisPlanValidator;
import com.sjeom.mydata.platform.analysis.validation.PlanValidationError;
import com.sjeom.mydata.platform.analysis.validation.PlanValidationResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AnalysisPlanJsonReader {

    private static final String SCHEMA_RESOURCE = "/schema/analysis-plan.schema.json";

    private final ObjectMapper objectMapper;
    private final Schema schema;
    private final AnalysisPlanValidator planValidator;

    public AnalysisPlanJsonReader() {
        this(defaultObjectMapper(), loadSchema(), new AnalysisPlanValidator());
    }

    AnalysisPlanJsonReader(
            ObjectMapper objectMapper,
            Schema schema,
            AnalysisPlanValidator planValidator
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.schema = Objects.requireNonNull(schema);
        this.planValidator = Objects.requireNonNull(planValidator);
    }

    public AnalysisPlanInputResult read(String json) {
        if (json == null || json.isBlank()) {
            return rejected("MALFORMED_JSON", "$", "JSON input must not be blank");
        }

        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            return rejected("MALFORMED_JSON", "$", "JSON input is malformed");
        }

        List<com.networknt.schema.Error> schemaErrors = schema.validate(jsonNode);
        if (!schemaErrors.isEmpty()) {
            List<AnalysisPlanInputError> errors = schemaErrors.stream()
                    .map(AnalysisPlanJsonReader::toInputError)
                    .sorted(Comparator
                            .comparing(AnalysisPlanInputError::field)
                            .thenComparing(AnalysisPlanInputError::code))
                    .toList();
            return AnalysisPlanInputResult.rejected(errors);
        }

        AnalysisPlan plan;
        try {
            plan = objectMapper.treeToValue(jsonNode, AnalysisPlan.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return rejected("PLAN_DESERIALIZATION_FAILED", "$", "JSON could not be converted to AnalysisPlan");
        }

        PlanValidationResult validationResult = planValidator.validate(plan);
        if (!validationResult.isValid()) {
            List<AnalysisPlanInputError> errors = validationResult.errors().stream()
                    .map(AnalysisPlanJsonReader::toInputError)
                    .toList();
            return AnalysisPlanInputResult.rejected(errors);
        }
        return AnalysisPlanInputResult.accepted(plan);
    }

    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    private static Schema loadSchema() {
        try (InputStream input = AnalysisPlanJsonReader.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("AnalysisPlan JSON Schema resource was not found");
            }
            String schemaData = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            SchemaRegistry registry = SchemaRegistry.withDefaultDialect(
                    SpecificationVersion.DRAFT_2020_12
            );
            return registry.getSchema(schemaData, InputFormat.JSON);
        } catch (IOException exception) {
            throw new IllegalStateException("AnalysisPlan JSON Schema could not be loaded", exception);
        }
    }

    private static AnalysisPlanInputError toInputError(com.networknt.schema.Error error) {
        String keyword = error.getKeyword() == null ? "UNKNOWN" : error.getKeyword().toUpperCase();
        String field = error.getInstanceLocation() == null
                ? "$"
                : error.getInstanceLocation().toString();
        return new AnalysisPlanInputError(
                "JSON_SCHEMA_" + keyword,
                field.isBlank() ? "$" : field,
                error.getMessage()
        );
    }

    private static AnalysisPlanInputError toInputError(PlanValidationError error) {
        return new AnalysisPlanInputError(error.code(), error.field(), error.code());
    }

    private static AnalysisPlanInputResult rejected(String code, String field, String message) {
        return AnalysisPlanInputResult.rejected(List.of(
                new AnalysisPlanInputError(code, field, message)
        ));
    }
}
