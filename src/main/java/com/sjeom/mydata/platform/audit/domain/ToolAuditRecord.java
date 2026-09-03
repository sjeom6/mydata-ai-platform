package com.sjeom.mydata.platform.audit.domain;

import com.sjeom.mydata.platform.tool.domain.ToolExecutionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ToolAuditRecord(
        String toolName,
        ToolExecutionStatus status,
        Map<String, String> inputSummary,
        Map<String, String> outputSummary,
        List<String> reasonCodes,
        Instant executedAt
) {

    public ToolAuditRecord {
        inputSummary = Map.copyOf(inputSummary);
        outputSummary = Map.copyOf(outputSummary);
        reasonCodes = List.copyOf(reasonCodes);
    }
}
