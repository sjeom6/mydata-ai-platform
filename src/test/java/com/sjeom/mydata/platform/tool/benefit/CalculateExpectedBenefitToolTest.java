package com.sjeom.mydata.platform.tool.benefit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sjeom.mydata.platform.product.domain.ExpectedBenefit;
import com.sjeom.mydata.platform.product.domain.ProductCandidate;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionResult;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionStatus;
import com.sjeom.mydata.platform.tool.product.CardProductSearchResult;
import com.sjeom.mydata.platform.tool.segment.CustomerSegment;
import com.sjeom.mydata.platform.tool.segment.CustomerSegmentMember;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalculateExpectedBenefitToolTest {

    private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void calculatesAnnualBenefitWithPerformanceConditionAndMonthlyLimit() {
        CalculateExpectedBenefitTool tool = new CalculateExpectedBenefitTool(CLOCK);
        CalculateExpectedBenefitInput input = new CalculateExpectedBenefitInput(
                segment(ConsumptionCategory.CAFE, List.of(member("CUST-A", "150000"))),
                products(ConsumptionCategory.CAFE, List.of(
                        product("CARD-A", ConsumptionCategory.CAFE, "0.10", "10000", "300000"),
                        product("CARD-B", ConsumptionCategory.CAFE, "0.05", "20000", "0")
                )),
                Map.of("CUST-A", new BigDecimal("400000"))
        );

        ToolExecutionResult<BenefitCalculationResult> result = tool.execute(input, context(10));

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
        assertThat(result.output().currency()).isEqualTo("KRW");
        assertThat(result.output().projectionMonths()).isEqualTo(12);
        assertThat(result.output().benefits())
                .extracting(ExpectedBenefit::productId)
                .containsExactly("CARD-A", "CARD-B");

        ExpectedBenefit cardA = result.output().benefits().getFirst();
        assertThat(cardA.eligible()).isTrue();
        assertThat(cardA.monthlyExpectedBenefit()).isEqualByComparingTo("10000");
        assertThat(cardA.annualExpectedBenefit()).isEqualByComparingTo("120000");
        assertThat(cardA.reasonCodes())
                .containsExactly("PREVIOUS_MONTH_SPEND_MET", "MONTHLY_DISCOUNT_LIMIT_APPLIED");

        ExpectedBenefit cardB = result.output().benefits().get(1);
        assertThat(cardB.monthlyExpectedBenefit()).isEqualByComparingTo("7500");
        assertThat(cardB.annualExpectedBenefit()).isEqualByComparingTo("90000");
    }

    @Test
    void marksCustomerIneligibleWhenPreviousMonthSpendIsMissing() {
        CalculateExpectedBenefitTool tool = new CalculateExpectedBenefitTool(CLOCK);
        CalculateExpectedBenefitInput input = new CalculateExpectedBenefitInput(
                segment(ConsumptionCategory.CAFE, List.of(member("CUST-A", "150000"))),
                products(ConsumptionCategory.CAFE, List.of(
                        product("CARD-A", ConsumptionCategory.CAFE, "0.10", "10000", "300000")
                )),
                Map.of()
        );

        ExpectedBenefit benefit = tool.execute(input, context(10)).output().benefits().getFirst();

        assertThat(benefit.eligible()).isFalse();
        assertThat(benefit.monthlyExpectedBenefit()).isEqualByComparingTo("0");
        assertThat(benefit.annualExpectedBenefit()).isEqualByComparingTo("0");
        assertThat(benefit.reasonCodes()).containsExactly("PREVIOUS_MONTH_SPEND_NOT_MET");
    }

    @Test
    void rejectsMismatchedSegmentAndProductCategory() {
        CalculateExpectedBenefitTool tool = new CalculateExpectedBenefitTool(CLOCK);
        CalculateExpectedBenefitInput input = new CalculateExpectedBenefitInput(
                segment(ConsumptionCategory.CAFE, List.of(member("CUST-A", "150000"))),
                products(ConsumptionCategory.TRAVEL, List.of(
                        product("CARD-A", ConsumptionCategory.TRAVEL, "0.10", "10000", "0")
                )),
                Map.of("CUST-A", BigDecimal.ZERO)
        );

        ToolExecutionResult<BenefitCalculationResult> result = tool.execute(input, context(10));

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.REJECTED);
        assertThat(result.reasonCodes()).containsExactly("CATEGORY_MISMATCH");
        assertThat(result.output()).isNull();
    }

    @Test
    void rejectsCustomerProductCombinationsAboveLimit() {
        CalculateExpectedBenefitTool tool = new CalculateExpectedBenefitTool(CLOCK);
        CalculateExpectedBenefitInput input = new CalculateExpectedBenefitInput(
                segment(ConsumptionCategory.CAFE, List.of(
                        member("CUST-A", "150000"),
                        member("CUST-B", "200000")
                )),
                products(ConsumptionCategory.CAFE, List.of(
                        product("CARD-A", ConsumptionCategory.CAFE, "0.10", "10000", "0"),
                        product("CARD-B", ConsumptionCategory.CAFE, "0.05", "20000", "0")
                )),
                Map.of()
        );

        ToolExecutionResult<BenefitCalculationResult> result = tool.execute(input, context(3));

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.REJECTED);
        assertThat(result.reasonCodes()).containsExactly("RESULT_LIMIT_EXCEEDED");
    }

    private static CustomerSegment segment(
            ConsumptionCategory category,
            List<CustomerSegmentMember> members
    ) {
        return new CustomerSegment(
                "COFFEE_HEAVY_USER",
                category,
                new BigDecimal("100000"),
                members
        );
    }

    private static CustomerSegmentMember member(String customerKey, String monthlyAverage) {
        BigDecimal average = new BigDecimal(monthlyAverage);
        return new CustomerSegmentMember(
                customerKey,
                average.multiply(BigDecimal.valueOf(3)),
                average,
                10,
                List.of("CAFE_MONTHLY_AVG_THRESHOLD_MET")
        );
    }

    private static CardProductSearchResult products(
            ConsumptionCategory category,
            List<ProductCandidate> candidates
    ) {
        return new CardProductSearchResult(category, LocalDate.of(2026, 9, 3), candidates);
    }

    private static ProductCandidate product(
            String productId,
            ConsumptionCategory category,
            String discountRate,
            String monthlyLimit,
            String minimumPreviousMonthSpend
    ) {
        return new ProductCandidate(
                productId,
                productId + " name",
                category,
                new BigDecimal(discountRate),
                new BigDecimal(monthlyLimit),
                new BigDecimal(minimumPreviousMonthSpend)
        );
    }

    private static ToolExecutionContext context(int maxResultCount) {
        return new ToolExecutionContext(
                UUID.fromString("a12c1de7-ce9f-4a21-b8cc-c66b085d5a8d"),
                "business-user",
                "CARD_RECOMMENDATION",
                NOW,
                LocalDate.of(2026, 9, 3),
                maxResultCount
        );
    }
}
