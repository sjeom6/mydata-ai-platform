package com.sjeom.mydata.platform.analysis.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.sjeom.mydata.platform.analysis.domain.AnalysisCondition;
import com.sjeom.mydata.platform.analysis.domain.AnalysisPeriod;
import com.sjeom.mydata.platform.analysis.domain.AnalysisPlan;
import com.sjeom.mydata.platform.analysis.domain.ComparisonOperator;
import com.sjeom.mydata.platform.analysis.domain.ConditionMetric;
import com.sjeom.mydata.platform.analysis.domain.PeriodType;
import com.sjeom.mydata.platform.analysis.domain.PolicyStatus;
import com.sjeom.mydata.platform.analysis.domain.ProductMatching;
import com.sjeom.mydata.platform.analysis.domain.ProductType;
import com.sjeom.mydata.platform.analysis.domain.RankingMetric;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisPlanValidatorTest {

    private final AnalysisPlanValidator validator = new AnalysisPlanValidator();

    @Test
    void acceptsSupportedCoffeeRecommendationPlan() {
        assertThat(validator.validate(plan(3, PolicyStatus.CURRENTLY_ALLOWED)).isValid()).isTrue();
    }

    @Test
    void rejectsPolicyThatRequiresReview() {
        PlanValidationResult result = validator.validate(plan(3, PolicyStatus.REVIEW_REQUIRED));

        assertThat(result.errors())
                .extracting(PlanValidationError::code)
                .containsExactly("POLICY_NOT_ALLOWED");
    }

    @Test
    void rejectsPeriodAboveAllowlistLimit() {
        PlanValidationResult result = validator.validate(plan(13, PolicyStatus.CURRENTLY_ALLOWED));

        assertThat(result.errors())
                .extracting(PlanValidationError::code)
                .containsExactly("PERIOD_LIMIT_EXCEEDED");
    }

    @Test
    void rejectsUnknownOrReorderedToolSequence() {
        AnalysisPlan source = plan(3, PolicyStatus.CURRENTLY_ALLOWED);
        AnalysisPlan invalid = new AnalysisPlan(
                source.planId(),
                source.planVersion(),
                source.segmentCode(),
                source.period(),
                source.conditions(),
                List.of("UNKNOWN_TOOL"),
                source.productMatching(),
                source.policyStatus()
        );

        PlanValidationResult result = validator.validate(invalid);

        assertThat(result.errors())
                .extracting(PlanValidationError::code)
                .containsExactly("UNSUPPORTED_TOOL_SEQUENCE");
    }

    private static AnalysisPlan plan(int months, PolicyStatus policyStatus) {
        return new AnalysisPlan(
                "PLAN-20260903-0001",
                "1.0",
                "COFFEE_HEAVY_USER",
                new AnalysisPeriod(PeriodType.RELATIVE_MONTH, months),
                List.of(new AnalysisCondition(
                        ConsumptionCategory.CAFE,
                        ConditionMetric.MONTHLY_AVG_AMOUNT,
                        ComparisonOperator.GTE,
                        new BigDecimal("100000"),
                        "KRW"
                )),
                AnalysisPlanValidator.SUPPORTED_TOOL_SEQUENCE,
                new ProductMatching(
                        ProductType.CREDIT_CARD,
                        ConsumptionCategory.CAFE,
                        RankingMetric.EXPECTED_ANNUAL_BENEFIT
                ),
                policyStatus
        );
    }
}
