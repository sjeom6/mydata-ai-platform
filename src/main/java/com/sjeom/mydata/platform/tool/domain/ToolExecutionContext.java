package com.sjeom.mydata.platform.tool.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record ToolExecutionContext(
        UUID executionId,
        String requesterId,
        String purpose,
        Instant requestedAt,
        LocalDate dataAsOf,
        int maxResultCount
) {

    public ToolExecutionContext {
        Objects.requireNonNull(executionId, "executionId must not be null");
        requesterId = requireText(requesterId, "requesterId");
        purpose = requireText(purpose, "purpose");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        Objects.requireNonNull(dataAsOf, "dataAsOf must not be null");

        if (maxResultCount <= 0) {
            throw new IllegalArgumentException("maxResultCount must be positive");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
