package com.sjeom.mydata.platform.analysis.validation;

import com.sjeom.mydata.platform.analysis.domain.AnalysisCondition;
import com.sjeom.mydata.platform.analysis.domain.AnalysisPlan;
import com.sjeom.mydata.platform.analysis.domain.ComparisonOperator;
import com.sjeom.mydata.platform.analysis.domain.ConditionMetric;
import com.sjeom.mydata.platform.analysis.domain.PeriodType;
import com.sjeom.mydata.platform.analysis.domain.PolicyStatus;
import com.sjeom.mydata.platform.analysis.domain.ProductType;
import com.sjeom.mydata.platform.analysis.domain.RankingMetric;
import com.sjeom.mydata.platform.tool.benefit.CalculateExpectedBenefitTool;
import com.sjeom.mydata.platform.tool.consumption.GetConsumptionSummaryTool;
import com.sjeom.mydata.platform.tool.product.SearchCardProductsTool;
import com.sjeom.mydata.platform.tool.recommendation.RankRecommendationsTool;
import com.sjeom.mydata.platform.tool.segment.FilterCustomerSegmentTool;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AnalysisPlanValidator {

    public static final String SUPPORTED_VERSION = "1.0";
    public static final int MAX_PERIOD_MONTHS = 12;
    public static final BigDecimal MAX_THRESHOLD_AMOUNT = new BigDecimal("100000000");

    public static final List<String> SUPPORTED_TOOL_SEQUENCE = List.of(
            GetConsumptionSummaryTool.NAME,
            FilterCustomerSegmentTool.NAME,
            SearchCardProductsTool.NAME,
            CalculateExpectedBenefitTool.NAME,
            RankRecommendationsTool.NAME
    );

    public PlanValidationResult validate(AnalysisPlan plan) {
        Objects.requireNonNull(plan, "plan must not be null");
        List<PlanValidationError> errors = new ArrayList<>();

        if (!SUPPORTED_VERSION.equals(plan.planVersion())) {
            errors.add(error("UNSUPPORTED_PLAN_VERSION", "planVersion"));
        }
        if (plan.policyStatus() != PolicyStatus.CURRENTLY_ALLOWED) {
            errors.add(error("POLICY_NOT_ALLOWED", "policyStatus"));
        }
        if (plan.period().type() != PeriodType.RELATIVE_MONTH) {
            errors.add(error("UNSUPPORTED_PERIOD_TYPE", "period.type"));
        }
        if (plan.period().value() > MAX_PERIOD_MONTHS) {
            errors.add(error("PERIOD_LIMIT_EXCEEDED", "period.value"));
        }
        if (!SUPPORTED_TOOL_SEQUENCE.equals(plan.toolSteps())) {
            errors.add(error("UNSUPPORTED_TOOL_SEQUENCE", "toolSteps"));
        }
        if (plan.conditions().size() != 1) {
            errors.add(error("UNSUPPORTED_CONDITION_COUNT", "conditions"));
            return new PlanValidationResult(errors);
        }

        AnalysisCondition condition = plan.conditions().getFirst();
        if (condition.metric() != ConditionMetric.MONTHLY_AVG_AMOUNT
                || condition.operator() != ComparisonOperator.GTE) {
            errors.add(error("UNSUPPORTED_CONDITION", "conditions[0]"));
        }
        if (condition.value().compareTo(MAX_THRESHOLD_AMOUNT) > 0) {
            errors.add(error("THRESHOLD_LIMIT_EXCEEDED", "conditions[0].value"));
        }
        if (!"KRW".equals(condition.currency())) {
            errors.add(error("UNSUPPORTED_CURRENCY", "conditions[0].currency"));
        }
        if (plan.productMatching().productType() != ProductType.CREDIT_CARD
                || plan.productMatching().rankingMetric() != RankingMetric.EXPECTED_ANNUAL_BENEFIT) {
            errors.add(error("UNSUPPORTED_PRODUCT_MATCHING", "productMatching"));
        }
        if (condition.category() != plan.productMatching().benefitCategory()) {
            errors.add(error("CATEGORY_MISMATCH", "productMatching.benefitCategory"));
        }
        return new PlanValidationResult(errors);
    }

    private static PlanValidationError error(String code, String field) {
        return new PlanValidationError(code, field);
    }
}
