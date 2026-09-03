package com.sjeom.mydata.platform.analysis.application;

import com.sjeom.mydata.platform.analysis.domain.AnalysisPlan;
import com.sjeom.mydata.platform.analysis.input.AnalysisPlanInputResult;
import com.sjeom.mydata.platform.analysis.input.AnalysisPlanJsonReader;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AnalysisPlanExecutionFacade {

    private final AnalysisPlanJsonReader jsonReader;
    private final AnalysisPlanRegistrationService registrationService;
    private final FixedPlanExecutionService executionService;
    private final PreviousMonthSpendProvider previousMonthSpendProvider;
    private final Clock clock;

    public AnalysisPlanExecutionFacade(
            AnalysisPlanJsonReader jsonReader,
            AnalysisPlanRegistrationService registrationService,
            FixedPlanExecutionService executionService,
            PreviousMonthSpendProvider previousMonthSpendProvider,
            Clock clock
    ) {
        this.jsonReader = Objects.requireNonNull(jsonReader);
        this.registrationService = Objects.requireNonNull(registrationService);
        this.executionService = Objects.requireNonNull(executionService);
        this.previousMonthSpendProvider = Objects.requireNonNull(previousMonthSpendProvider);
        this.clock = Objects.requireNonNull(clock);
    }

    public AnalysisRequestResult execute(
            String planJson,
            String requesterId,
            String purpose,
            LocalDate dataAsOf,
            int maxResultCount
    ) {
        UUID executionId = UUID.randomUUID();
        AnalysisPlanInputResult inputResult = jsonReader.read(planJson);
        if (!inputResult.isAccepted()) {
            return new AnalysisRequestResult(
                    executionId,
                    AnalysisRequestStatus.INVALID_PLAN,
                    null,
                    null,
                    inputResult.errors(),
                    List.of()
            );
        }

        AnalysisPlan plan = inputResult.plan();
        ToolExecutionContext context = new ToolExecutionContext(
                executionId,
                requesterId,
                purpose,
                clock.instant(),
                dataAsOf,
                maxResultCount
        );
        PlanRegistrationResult registrationResult = registrationService.register(plan, context);
        if (registrationResult.status() == PlanRegistrationStatus.CONFLICT) {
            return new AnalysisRequestResult(
                    executionId,
                    AnalysisRequestStatus.PLAN_CONFLICT,
                    plan.planId(),
                    null,
                    List.of(),
                    registrationResult.reasonCodes()
            );
        }
        if (registrationResult.status() == PlanRegistrationStatus.REJECTED) {
            return new AnalysisRequestResult(
                    executionId,
                    AnalysisRequestStatus.INVALID_PLAN,
                    plan.planId(),
                    null,
                    List.of(),
                    registrationResult.reasonCodes()
            );
        }

        AnalysisExecutionResult executionResult = executionService.execute(
                plan,
                context,
                previousMonthSpendProvider.findByDataAsOf(dataAsOf)
        );
        return new AnalysisRequestResult(
                executionId,
                mapStatus(executionResult.status()),
                plan.planId(),
                executionResult.output(),
                List.of(),
                executionResult.reasonCodes()
        );
    }

    private static AnalysisRequestStatus mapStatus(AnalysisExecutionStatus status) {
        return switch (status) {
            case SUCCESS -> AnalysisRequestStatus.SUCCESS;
            case REJECTED -> AnalysisRequestStatus.EXECUTION_REJECTED;
            case FAILED -> AnalysisRequestStatus.FAILED;
        };
    }
}
