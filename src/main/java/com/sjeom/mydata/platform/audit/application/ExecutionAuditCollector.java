package com.sjeom.mydata.platform.audit.application;

import com.sjeom.mydata.platform.analysis.application.AnalysisExecutionResult;
import com.sjeom.mydata.platform.analysis.application.AnalysisExecutionStatus;
import com.sjeom.mydata.platform.analysis.domain.AnalysisPlan;
import com.sjeom.mydata.platform.audit.domain.AuditOutcome;
import com.sjeom.mydata.platform.audit.domain.AuditRecord;
import com.sjeom.mydata.platform.audit.domain.ToolAuditRecord;
import com.sjeom.mydata.platform.audit.persistence.AuditRecordRepository;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionResult;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ExecutionAuditCollector {

    public static final String RULE_VERSION = "POC-RULE-1.0";
    public static final String NOT_USED = "NOT_USED";

    private final AnalysisPlan plan;
    private final ToolExecutionContext context;
    private final AuditRecordRepository repository;
    private final Clock clock;
    private final Instant startedAt;
    private final List<ToolAuditRecord> toolExecutions = new ArrayList<>();

    public ExecutionAuditCollector(
            AnalysisPlan plan,
            ToolExecutionContext context,
            AuditRecordRepository repository,
            Clock clock
    ) {
        this.plan = Objects.requireNonNull(plan);
        this.context = Objects.requireNonNull(context);
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
        this.startedAt = clock.instant();
    }

    public void recordTool(
            ToolExecutionResult<?> result,
            Map<String, String> inputSummary,
            Map<String, String> outputSummary
    ) {
        toolExecutions.add(new ToolAuditRecord(
                result.toolName(),
                result.status(),
                inputSummary,
                outputSummary,
                result.reasonCodes(),
                result.executedAt()
        ));
    }

    public AnalysisExecutionResult complete(AnalysisExecutionResult result) {
        AuditRecord record = new AuditRecord(
                context.executionId(),
                plan.planId(),
                plan.planVersion(),
                context.requesterId(),
                context.purpose(),
                context.dataAsOf(),
                NOT_USED,
                NOT_USED,
                RULE_VERSION,
                toAuditOutcome(result.status()),
                toolExecutions,
                result.reasonCodes(),
                startedAt,
                clock.instant()
        );
        repository.save(record);
        return result;
    }

    private static AuditOutcome toAuditOutcome(AnalysisExecutionStatus status) {
        return switch (status) {
            case SUCCESS -> AuditOutcome.SUCCESS;
            case REJECTED -> AuditOutcome.REJECTED;
            case FAILED -> AuditOutcome.FAILED;
        };
    }
}
