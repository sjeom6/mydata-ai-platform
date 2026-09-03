package com.sjeom.mydata.platform.analysis.validation;

import java.util.List;

public record PlanValidationResult(List<PlanValidationError> errors) {

    public PlanValidationResult {
        errors = List.copyOf(errors);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
