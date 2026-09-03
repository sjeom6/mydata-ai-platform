package com.sjeom.mydata.platform.analysis.application;

import com.sjeom.mydata.platform.analysis.persistence.StoredAnalysisPlan;
import java.util.List;

public record PlanRegistrationResult(
        PlanRegistrationStatus status,
        StoredAnalysisPlan storedPlan,
        List<String> reasonCodes
) {

    public PlanRegistrationResult {
        if (status == null) {
            throw new IllegalArgumentException("registration status must not be null");
        }
        reasonCodes = List.copyOf(reasonCodes);
    }
}
