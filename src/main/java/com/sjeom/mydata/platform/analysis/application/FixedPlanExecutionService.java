package com.sjeom.mydata.platform.analysis.application;

import com.sjeom.mydata.platform.analysis.domain.AnalysisCondition;
import com.sjeom.mydata.platform.analysis.domain.AnalysisPlan;
import com.sjeom.mydata.platform.analysis.validation.AnalysisPlanValidator;
import com.sjeom.mydata.platform.analysis.validation.PlanValidationError;
import com.sjeom.mydata.platform.analysis.validation.PlanValidationResult;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FixedPlanExecutionService {

    public static final String NAME = "FIXED_PLAN_EXECUTION";

    private final AnalysisPlanValidator planValidator;
    private final GetConsumptionSummaryTool consumptionSummaryTool;
    private final FilterCustomerSegmentTool customerSegmentTool;
    private final SearchCardProductsTool cardProductsTool;
    private final CalculateExpectedBenefitTool expectedBenefitTool;
    private final RankRecommendationsTool recommendationsTool;

    public FixedPlanExecutionService(
            AnalysisPlanValidator planValidator,
            GetConsumptionSummaryTool consumptionSummaryTool,
            FilterCustomerSegmentTool customerSegmentTool,
            SearchCardProductsTool cardProductsTool,
            CalculateExpectedBenefitTool expectedBenefitTool,
            RankRecommendationsTool recommendationsTool
    ) {
        this.planValidator = Objects.requireNonNull(planValidator);
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

        PlanValidationResult validationResult = planValidator.validate(plan);
        if (!validationResult.isValid()) {
            List<String> reasonCodes = validationResult.errors().stream()
                    .map(PlanValidationError::code)
                    .toList();
            return AnalysisExecutionResult.rejected(plan.planId(), "PLAN_VALIDATION", reasonCodes);
        }
        AnalysisCondition condition = plan.conditions().getFirst();

        ToolExecutionResult<ConsumptionSummary> consumptionResult = consumptionSummaryTool.execute(
                new ConsumptionSummaryInput(plan.period().value(), condition.category()),
                context
        );
        if (consumptionResult.status() != ToolExecutionStatus.SUCCESS) {
            return stopped(plan.planId(), consumptionSummaryTool.name(), consumptionResult);
        }

        ToolExecutionResult<CustomerSegment> segmentResult = customerSegmentTool.execute(
                new FilterCustomerSegmentInput(
                        plan.segmentCode(),
                        consumptionResult.output(),
                        condition.value()
                ),
                context
        );
        if (segmentResult.status() != ToolExecutionStatus.SUCCESS) {
            return stopped(plan.planId(), customerSegmentTool.name(), segmentResult);
        }

        ToolExecutionResult<CardProductSearchResult> productsResult = cardProductsTool.execute(
                new SearchCardProductsInput(plan.productMatching().benefitCategory()),
                context
        );
        if (productsResult.status() != ToolExecutionStatus.SUCCESS) {
            return stopped(plan.planId(), cardProductsTool.name(), productsResult);
        }

        ToolExecutionResult<BenefitCalculationResult> benefitResult = expectedBenefitTool.execute(
                new CalculateExpectedBenefitInput(
                        segmentResult.output(),
                        productsResult.output(),
                        previousMonthSpendByCustomer
                ),
                context
        );
        if (benefitResult.status() != ToolExecutionStatus.SUCCESS) {
            return stopped(plan.planId(), expectedBenefitTool.name(), benefitResult);
        }

        ToolExecutionResult<RecommendationRankingResult> recommendationResult = recommendationsTool.execute(
                new RankRecommendationsInput(benefitResult.output()),
                context
        );
        if (recommendationResult.status() != ToolExecutionStatus.SUCCESS) {
            return stopped(plan.planId(), recommendationsTool.name(), recommendationResult);
        }

        return AnalysisExecutionResult.success(plan.planId(), recommendationResult.output());
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
