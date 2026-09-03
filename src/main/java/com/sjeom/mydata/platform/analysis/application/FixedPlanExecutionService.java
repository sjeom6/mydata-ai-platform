package com.sjeom.mydata.platform.analysis.application;

import com.sjeom.mydata.platform.analysis.domain.AnalysisCondition;
import com.sjeom.mydata.platform.analysis.domain.AnalysisPlan;
import com.sjeom.mydata.platform.analysis.validation.AnalysisPlanValidator;
import com.sjeom.mydata.platform.analysis.validation.PlanValidationError;
import com.sjeom.mydata.platform.analysis.validation.PlanValidationResult;
import com.sjeom.mydata.platform.audit.application.ExecutionAuditCollector;
import com.sjeom.mydata.platform.audit.persistence.AuditRecordRepository;
import com.sjeom.mydata.platform.tool.benefit.BenefitCalculationResult;
import com.sjeom.mydata.platform.tool.benefit.CalculateExpectedBenefitInput;
import com.sjeom.mydata.platform.tool.benefit.CalculateExpectedBenefitTool;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionSummary;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionSummaryInput;
import com.sjeom.mydata.platform.tool.consumption.GetConsumptionSummaryTool;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionResult;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionStatus;
import com.sjeom.mydata.platform.tool.product.CardProductSearchResult;
import com.sjeom.mydata.platform.tool.product.SearchCardProductsInput;
import com.sjeom.mydata.platform.tool.product.SearchCardProductsTool;
import com.sjeom.mydata.platform.tool.recommendation.RankRecommendationsInput;
import com.sjeom.mydata.platform.tool.recommendation.RankRecommendationsTool;
import com.sjeom.mydata.platform.tool.recommendation.RecommendationRankingResult;
import com.sjeom.mydata.platform.tool.segment.CustomerSegment;
import com.sjeom.mydata.platform.tool.segment.FilterCustomerSegmentInput;
import com.sjeom.mydata.platform.tool.segment.FilterCustomerSegmentTool;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FixedPlanExecutionService {

    public static final String NAME = "FIXED_PLAN_EXECUTION";

    private final AnalysisPlanValidator planValidator;
    private final AuditRecordRepository auditRecordRepository;
    private final Clock clock;
    private final GetConsumptionSummaryTool consumptionSummaryTool;
    private final FilterCustomerSegmentTool customerSegmentTool;
    private final SearchCardProductsTool cardProductsTool;
    private final CalculateExpectedBenefitTool expectedBenefitTool;
    private final RankRecommendationsTool recommendationsTool;

    public FixedPlanExecutionService(
            AnalysisPlanValidator planValidator,
            AuditRecordRepository auditRecordRepository,
            Clock clock,
            GetConsumptionSummaryTool consumptionSummaryTool,
            FilterCustomerSegmentTool customerSegmentTool,
            SearchCardProductsTool cardProductsTool,
            CalculateExpectedBenefitTool expectedBenefitTool,
            RankRecommendationsTool recommendationsTool
    ) {
        this.planValidator = Objects.requireNonNull(planValidator);
        this.auditRecordRepository = Objects.requireNonNull(auditRecordRepository);
        this.clock = Objects.requireNonNull(clock);
        this.consumptionSummaryTool = Objects.requireNonNull(consumptionSummaryTool);
        this.customerSegmentTool = Objects.requireNonNull(customerSegmentTool);
        this.cardProductsTool = Objects.requireNonNull(cardProductsTool);
        this.expectedBenefitTool = Objects.requireNonNull(expectedBenefitTool);
        this.recommendationsTool = Objects.requireNonNull(recommendationsTool);
    }

    public AnalysisExecutionResult execute(
            AnalysisPlan plan,
            ToolExecutionContext context,
            Map<String, BigDecimal> previousMonthSpendByCustomer
    ) {
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(previousMonthSpendByCustomer, "previousMonthSpendByCustomer must not be null");

        ExecutionAuditCollector audit = new ExecutionAuditCollector(
                plan,
                context,
                auditRecordRepository,
                clock
        );

        PlanValidationResult validationResult = planValidator.validate(plan);
        if (!validationResult.isValid()) {
            List<String> reasonCodes = validationResult.errors().stream()
                    .map(PlanValidationError::code)
                    .toList();
            return audit.complete(AnalysisExecutionResult.rejected(
                    plan.planId(),
                    "PLAN_VALIDATION",
                    reasonCodes
            ));
        }
        AnalysisCondition condition = plan.conditions().getFirst();

        ToolExecutionResult<ConsumptionSummary> consumptionResult = consumptionSummaryTool.execute(
                new ConsumptionSummaryInput(plan.period().value(), condition.category()),
                context
        );
        audit.recordTool(
                consumptionResult,
                Map.of(
                        "months", Integer.toString(plan.period().value()),
                        "category", condition.category().name()
                ),
                consumptionSummary(consumptionResult.output())
        );
        if (consumptionResult.status() != ToolExecutionStatus.SUCCESS) {
            return audit.complete(stopped(plan.planId(), consumptionSummaryTool.name(), consumptionResult));
        }

        ToolExecutionResult<CustomerSegment> segmentResult = customerSegmentTool.execute(
                new FilterCustomerSegmentInput(
                        plan.segmentCode(),
                        consumptionResult.output(),
                        condition.value()
                ),
                context
        );
        audit.recordTool(
                segmentResult,
                Map.of(
                        "segmentCode", plan.segmentCode(),
                        "minimumMonthlyAverageAmount", condition.value().toPlainString()
                ),
                segmentSummary(segmentResult.output())
        );
        if (segmentResult.status() != ToolExecutionStatus.SUCCESS) {
            return audit.complete(stopped(plan.planId(), customerSegmentTool.name(), segmentResult));
        }

        ToolExecutionResult<CardProductSearchResult> productsResult = cardProductsTool.execute(
                new SearchCardProductsInput(plan.productMatching().benefitCategory()),
                context
        );
        audit.recordTool(
                productsResult,
                Map.of("benefitCategory", plan.productMatching().benefitCategory().name()),
                productSummary(productsResult.output())
        );
        if (productsResult.status() != ToolExecutionStatus.SUCCESS) {
            return audit.complete(stopped(plan.planId(), cardProductsTool.name(), productsResult));
        }

        ToolExecutionResult<BenefitCalculationResult> benefitResult = expectedBenefitTool.execute(
                new CalculateExpectedBenefitInput(
                        segmentResult.output(),
                        productsResult.output(),
                        previousMonthSpendByCustomer
                ),
                context
        );
        audit.recordTool(
                benefitResult,
                Map.of(
                        "segmentMemberCount", Integer.toString(segmentResult.output().members().size()),
                        "productCandidateCount", Integer.toString(productsResult.output().candidates().size())
                ),
                benefitSummary(benefitResult.output())
        );
        if (benefitResult.status() != ToolExecutionStatus.SUCCESS) {
            return audit.complete(stopped(plan.planId(), expectedBenefitTool.name(), benefitResult));
        }

        ToolExecutionResult<RecommendationRankingResult> recommendationResult = recommendationsTool.execute(
                new RankRecommendationsInput(benefitResult.output()),
                context
        );
        audit.recordTool(
                recommendationResult,
                Map.of("benefitCount", Integer.toString(benefitResult.output().benefits().size())),
                recommendationSummary(recommendationResult.output())
        );
        if (recommendationResult.status() != ToolExecutionStatus.SUCCESS) {
            return audit.complete(stopped(plan.planId(), recommendationsTool.name(), recommendationResult));
        }

        return audit.complete(AnalysisExecutionResult.success(plan.planId(), recommendationResult.output()));
    }

    private static Map<String, String> consumptionSummary(ConsumptionSummary output) {
        if (output == null) {
            return Map.of();
        }
        return Map.of(
                "customerCount", Integer.toString(output.customers().size()),
                "periodStart", output.periodStart().toString(),
                "periodEnd", output.periodEnd().toString()
        );
    }

    private static Map<String, String> segmentSummary(CustomerSegment output) {
        return output == null
                ? Map.of()
                : Map.of("selectedCustomerCount", Integer.toString(output.members().size()));
    }

    private static Map<String, String> productSummary(CardProductSearchResult output) {
        return output == null
                ? Map.of()
                : Map.of("candidateCount", Integer.toString(output.candidates().size()));
    }

    private static Map<String, String> benefitSummary(BenefitCalculationResult output) {
        if (output == null) {
            return Map.of();
        }
        long eligibleCount = output.benefits().stream().filter(benefit -> benefit.eligible()).count();
        return Map.of(
                "calculationCount", Integer.toString(output.benefits().size()),
                "eligibleCount", Long.toString(eligibleCount)
        );
    }

    private static Map<String, String> recommendationSummary(RecommendationRankingResult output) {
        if (output == null) {
            return Map.of();
        }
        return Map.of(
                "recommendationCount", Integer.toString(output.recommendations().size()),
                "noRecommendationCount", Integer.toString(output.noRecommendations().size())
        );
    }

    private static AnalysisExecutionResult stopped(
            String planId,
            String failedStep,
            ToolExecutionResult<?> toolResult
    ) {
        AnalysisExecutionStatus status = toolResult.status() == ToolExecutionStatus.FAILED
                ? AnalysisExecutionStatus.FAILED
                : AnalysisExecutionStatus.REJECTED;
        return new AnalysisExecutionResult(
                planId,
                status,
                null,
                failedStep,
                toolResult.reasonCodes()
        );
    }
}
