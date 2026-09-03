package com.sjeom.mydata.platform.ai.llm;

import java.util.List;

public record LlmPlanRequest(
        String businessRequest,
        int attempt,
        List<String> validationErrors
) {
    public LlmPlanRequest {
        if (businessRequest == null || businessRequest.isBlank()) {
            throw new IllegalArgumentException("businessRequest must not be blank");
        }
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be positive");
        }
        validationErrors = List.copyOf(validationErrors);
    }
}
