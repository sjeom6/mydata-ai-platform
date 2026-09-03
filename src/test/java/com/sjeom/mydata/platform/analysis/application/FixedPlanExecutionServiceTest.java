package com.sjeom.mydata.platform.analysis.application;

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
import com.sjeom.mydata.platform.analysis.validation.AnalysisPlanValidator;
import com.sjeom.mydata.platform.product.domain.CardProduct;
import com.sjeom.mydata.platform.product.domain.ProductRecommendation;
import com.sjeom.mydata.platform.product.domain.ProductSaleStatus;
import com.sjeom.mydata.platform.support.fixture.InMemoryCardProductRepository;
import com.sjeom.mydata.platform.support.fixture.InMemoryConsumptionTransactionRepository;
import com.sjeom.mydata.platform.tool.benefit.CalculateExpectedBenefitTool;
import com.sjeom.mydata.platform.tool.consumption.CardTransaction;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import com.sjeom.mydata.platform.tool.consumption.GetConsumptionSummaryTool;
import com.sjeom.mydata.platform.tool.consumption.TransactionStatus;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import com.sjeom.mydata.platform.tool.product.SearchCardProductsTool;
import com.sjeom.mydata.platform.tool.recommendation.RankRecommendationsTool;
import com.sjeom.mydata.platform.tool.segment.FilterCustomerSegmentTool;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FixedPlanExecutionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void executesCoffeeCardRecommendationFromTransactionsToFinalRanking() {
        FixedPlanExecutionService service = service();

        AnalysisExecutionResult result = service.execute(
                plan(PolicyStatus.CURRENTLY_ALLOWED),
                context(),
                Map.of(
                        "CUST-A", new BigDecimal("400000"),
                        "CUST-B", new BigDecimal("100000"),
                        "CUST-C", new BigDecimal("500000")
                )
        );

        assertThat(result.status()).isEqualTo(AnalysisExecutionStatus.SUCCESS);
        assertThat(result.analysisPlanId()).isEqualTo("PLAN-20260903-0001");
        assertThat(result.output().recommendations())
                .extracting(ProductRecommendation::customerKey)
                .containsExactly("CUST-A", "CUST-B");

        ProductRecommendation customerA = result.output().recommendations().getFirst();
        assertThat(customerA.productId()).isEqualTo("CARD-A");
        assertThat(customerA.annualExpectedBenefit()).isEqualByComparingTo("120000");

        ProductRecommendation customerB = result.output().recommendations().get(1);
        assertThat(customerB.productId()).isEqualTo("CARD-B");
        assertThat(customerB.annualExpectedBenefit()).isEqualByComparingTo("66000");
        assertThat(result.output().noRecommendations()).isEmpty();
    }

    @Test
    void blocksPlanThatIsNotCurrentlyAllowedBeforeReadingData() {
        AnalysisExecutionResult result = service().execute(
                plan(PolicyStatus.REVIEW_REQUIRED),
                context(),
                Map.of()
        );

        assertThat(result.status()).isEqualTo(AnalysisExecutionStatus.REJECTED);
        assertThat(result.failedStep()).isEqualTo("PLAN_VALIDATION");
        assertThat(result.reasonCodes()).containsExactly("POLICY_NOT_ALLOWED");
        assertThat(result.output()).isNull();
    }

    private static FixedPlanExecutionService service() {
        InMemoryConsumptionTransactionRepository transactionRepository =
                new InMemoryConsumptionTransactionRepository(List.of(
                        approved("T-01", "CUST-A", "2026-07-10", "130000"),
                        approved("T-02", "CUST-A", "2026-08-10", "130000"),
                        approved("T-03", "CUST-A", "2026-09-01", "130000"),
                        approved("T-04", "CUST-B", "2026-07-11", "110000"),
                        approved("T-05", "CUST-B", "2026-08-11", "110000"),
                        approved("T-06", "CUST-B", "2026-09-02", "110000"),
                        approved("T-07", "CUST-C", "2026-07-12", "20000"),
                        approved("T-08", "CUST-C", "2026-08-12", "20000"),
                        approved("T-09", "CUST-C", "2026-09-03", "20000")
                ));
        InMemoryCardProductRepository productRepository = new InMemoryCardProductRepository(List.of(
                product("CARD-A", "0.10", "10000", "300000"),
                product("CARD-B", "0.05", "20000", "0")
        ));

        return new FixedPlanExecutionService(
                new AnalysisPlanValidator(),
                new GetConsumptionSummaryTool(transactionRepository, CLOCK),
                new FilterCustomerSegmentTool(CLOCK),
                new SearchCardProductsTool(productRepository, CLOCK),
                new CalculateExpectedBenefitTool(CLOCK),
                new RankRecommendationsTool(CLOCK)
        );
    }

    private static AnalysisPlan plan(PolicyStatus policyStatus) {
        return new AnalysisPlan(
                "PLAN-20260903-0001",
                "1.0",
                "COFFEE_HEAVY_USER",
                new AnalysisPeriod(PeriodType.RELATIVE_MONTH, 3),
                List.of(new AnalysisCondition(
                        ConsumptionCategory.CAFE,
                        ConditionMetric.MONTHLY_AVG_AMOUNT,
                        ComparisonOperator.GTE,
                        new BigDecimal("100000"),
                        "KRW"
                )),
                List.of(
                        GetConsumptionSummaryTool.NAME,
                        FilterCustomerSegmentTool.NAME,
                        SearchCardProductsTool.NAME,
                        CalculateExpectedBenefitTool.NAME,
                        RankRecommendationsTool.NAME
                ),
                new ProductMatching(
                        ProductType.CREDIT_CARD,
                        ConsumptionCategory.CAFE,
                        RankingMetric.EXPECTED_ANNUAL_BENEFIT
                ),
                policyStatus
        );
    }

    private static CardTransaction approved(
            String transactionId,
            String customerKey,
            String occurredOn,
            String amount
    ) {
        return new CardTransaction(
                transactionId,
                customerKey,
                LocalDate.parse(occurredOn),
                ConsumptionCategory.CAFE,
                new BigDecimal(amount),
                TransactionStatus.APPROVED,
                null
        );
    }

    private static CardProduct product(
            String productId,
            String discountRate,
            String monthlyLimit,
            String minimumPreviousMonthSpend
    ) {
        return new CardProduct(
                productId,
                productId + " name",
                ProductSaleStatus.ON_SALE,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                ConsumptionCategory.CAFE,
                new BigDecimal(discountRate),
                new BigDecimal(monthlyLimit),
                new BigDecimal(minimumPreviousMonthSpend),
                true
        );
    }

    private static ToolExecutionContext context() {
        return new ToolExecutionContext(
                UUID.fromString("a12c1de7-ce9f-4a21-b8cc-c66b085d5a8d"),
                "business-user",
                "CARD_RECOMMENDATION",
                NOW,
                LocalDate.of(2026, 9, 3),
                100
        );
    }
}
