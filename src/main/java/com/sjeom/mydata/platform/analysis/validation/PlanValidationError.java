package com.sjeom.mydata.platform.analysis.validation;

public record PlanValidationError(String code, String field) {

    public PlanValidationError {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("validation error code must not be blank");
        }
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("validation error field must not be blank");
        }
    }
}
