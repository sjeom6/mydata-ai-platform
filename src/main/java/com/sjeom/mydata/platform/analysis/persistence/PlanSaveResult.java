package com.sjeom.mydata.platform.analysis.persistence;

public record PlanSaveResult(
        PlanSaveStatus status,
        StoredAnalysisPlan storedPlan
) {

    public PlanSaveResult {
        if (status == null || storedPlan == null) {
            throw new IllegalArgumentException("save status and stored plan must not be null");
        }
    }
}
