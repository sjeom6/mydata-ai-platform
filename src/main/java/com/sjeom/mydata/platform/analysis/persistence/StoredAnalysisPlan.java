package com.sjeom.mydata.platform.analysis.persistence;

import com.sjeom.mydata.platform.analysis.domain.AnalysisPlan;
import java.time.Instant;
import java.time.LocalDate;

public record StoredAnalysisPlan(
        AnalysisPlan plan,
        String planSnapshotJson,
        LocalDate dataAsOf,
        String contentHash,
        String requesterId,
        String purpose,
        Instant createdAt
) {

    public StoredAnalysisPlan {
        if (plan == null || dataAsOf == null || createdAt == null) {
            throw new IllegalArgumentException("stored plan, dataAsOf and createdAt must not be null");
        }
        planSnapshotJson = requireText(planSnapshotJson, "planSnapshotJson");
        contentHash = requireText(contentHash, "contentHash");
        requesterId = requireText(requesterId, "requesterId");
        purpose = requireText(purpose, "purpose");
    }

    public String planId() {
        return plan.planId();
    }

    public String planVersion() {
        return plan.planVersion();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
