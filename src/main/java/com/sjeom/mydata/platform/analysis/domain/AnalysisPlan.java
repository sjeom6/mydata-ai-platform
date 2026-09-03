package com.sjeom.mydata.platform.analysis.domain;

import java.util.List;

public record AnalysisPlan(
        String planId,
        String planVersion,
        String segmentCode,
        AnalysisPeriod period,
        List<AnalysisCondition> conditions,
        List<String> toolSteps,
        ProductMatching productMatching,
        PolicyStatus policyStatus
) {

    public AnalysisPlan {
        planId = requireText(planId, "planId");
        planVersion = requireText(planVersion, "planVersion");
        segmentCode = requireText(segmentCode, "segmentCode");
        if (period == null || productMatching == null || policyStatus == null) {
            throw new IllegalArgumentException("plan period, product matching and policy status must not be null");
        }
        conditions = List.copyOf(conditions);
        toolSteps = List.copyOf(toolSteps);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
