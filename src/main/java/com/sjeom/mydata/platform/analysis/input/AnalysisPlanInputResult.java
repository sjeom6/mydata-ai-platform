package com.sjeom.mydata.platform.analysis.input;

import com.sjeom.mydata.platform.analysis.domain.AnalysisPlan;
import java.util.List;

public record AnalysisPlanInputResult(
        AnalysisPlan plan,
        List<AnalysisPlanInputError> errors
) {

    public AnalysisPlanInputResult {
        errors = List.copyOf(errors);
        if (plan != null && !errors.isEmpty()) {
            throw new IllegalArgumentException("accepted plan must not contain errors");
        }
        if (plan == null && errors.isEmpty()) {
            throw new IllegalArgumentException("rejected plan must contain errors");
        }
    }

    public static AnalysisPlanInputResult accepted(AnalysisPlan plan) {
        return new AnalysisPlanInputResult(plan, List.of());
    }

    public static AnalysisPlanInputResult rejected(List<AnalysisPlanInputError> errors) {
        return new AnalysisPlanInputResult(null, errors);
    }

    public boolean isAccepted() {
        return plan != null;
    }
}
