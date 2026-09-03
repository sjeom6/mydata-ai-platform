package com.sjeom.mydata.platform.tool.domain;

import java.time.Instant;
import java.util.List;

public record ToolExecutionResult<O>(
        String toolName,
        ToolExecutionStatus status,
        O output,
        List<String> reasonCodes,
        Instant executedAt
) {

    public ToolExecutionResult {
        toolName = requireText(toolName, "toolName");
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (executedAt == null) {
            throw new IllegalArgumentException("executedAt must not be null");
        }

        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        if (reasonCodes.stream().anyMatch(code -> code == null || code.isBlank())) {
            throw new IllegalArgumentException("reasonCodes must not contain blank values");
        }
        if (status == ToolExecutionStatus.SUCCESS && output == null) {
            throw new IllegalArgumentException("successful result must contain output");
        }
        if (status != ToolExecutionStatus.SUCCESS && output != null) {
            throw new IllegalArgumentException("non-successful result must not contain output");
        }
        if (status != ToolExecutionStatus.SUCCESS && reasonCodes.isEmpty()) {
            throw new IllegalArgumentException("non-successful result must contain a reason code");
        }
    }

    public static <O> ToolExecutionResult<O> success(String toolName, O output, Instant executedAt) {
        return new ToolExecutionResult<>(toolName, ToolExecutionStatus.SUCCESS, output, List.of(), executedAt);
    }

    public static <O> ToolExecutionResult<O> rejected(String toolName, String reasonCode, Instant executedAt) {
        return new ToolExecutionResult<>(toolName, ToolExecutionStatus.REJECTED, null, List.of(reasonCode), executedAt);
    }

    public static <O> ToolExecutionResult<O> failed(String toolName, String reasonCode, Instant executedAt) {
        return new ToolExecutionResult<>(toolName, ToolExecutionStatus.FAILED, null, List.of(reasonCode), executedAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
