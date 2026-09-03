package com.sjeom.mydata.platform.audit.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AuditRecord(
        UUID executionId,
        String analysisPlanId,
        String planVersion,
        String requesterId,
        String purpose,
        LocalDate dataAsOf,
        String modelVersion,
        String promptVersion,
        String ruleVersion,
        AuditOutcome outcome,
        List<ToolAuditRecord> toolExecutions,
        List<String> reasonCodes,
        Instant startedAt,
        Instant completedAt
) {

    public AuditRecord {
        toolExecutions = List.copyOf(toolExecutions);
        reasonCodes = List.copyOf(reasonCodes);
    }
}
