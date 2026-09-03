package com.sjeom.mydata.platform.tool.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.sjeom.mydata.platform.product.domain.ExpectedBenefit;
import com.sjeom.mydata.platform.product.domain.NoRecommendation;
import com.sjeom.mydata.platform.product.domain.ProductRecommendation;
import com.sjeom.mydata.platform.tool.benefit.BenefitCalculationResult;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionResult;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RankRecommendationsToolTest {

    private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void selectsHighestAnnualBenefitForEachCustomer() {
        RankRecommendationsTool tool = new RankRecommendationsTool(CLOCK);
        BenefitCalculationResult benefits = result(List.of(
                eligible("CUST-A", "CARD-A", "10000", "120000"),
                eligible("CUST-A", "CARD-B", "7500", "90000"),
                eligible("CUST-B", "CARD-C", "12000", "144000")
        ));

        ToolExecutionResult<RecommendationRankingResult> result = tool.execute(
                new RankRecommendationsInput(benefits),
                context(10)
        );

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
        assertThat(result.output().rankingMetric()).isEqualTo("EXPECTED_ANNUAL_BENEFIT");
        assertThat(result.output().recommendations())
                .extracting(ProductRecommendation::productId)
                .containsExactly("CARD-A", "CARD-C");
        assertThat(result.output().noRecommendations()).isEmpty();
    }

    @Test
    void breaksBenefitTieByProductId() {
        RankRecommendationsTool tool = new RankRecommendationsTool(CLOCK);
        BenefitCalculationResult benefits = result(List.of(
                eligible("CUST-A", "CARD-B", "10000", "120000"),
                eligible("CUST-A", "CARD-A", "10000", "120000")
        ));

        ProductRecommendation recommendation = tool.execute(
                new RankRecommendationsInput(benefits),
                context(10)
        ).output().recommendations().getFirst();

        assertThat(recommendation.productId()).isEqualTo("CARD-A");
        assertThat(recommendation.reasonCodes())
                .containsExactly("PREVIOUS_MONTH_SPEND_MET", "HIGHEST_EXPECTED_ANNUAL_BENEFIT");
    }

    @Test
    void recordsCustomerWhenNoProductIsEligible() {
        RankRecommendationsTool tool = new RankRecommendationsTool(CLOCK);
        BenefitCalculationResult benefits = result(List.of(
                ineligible("CUST-A", "CARD-A"),
                ineligible("CUST-A", "CARD-B")
        ));

        RecommendationRankingResult output = tool.execute(
                new RankRecommendationsInput(benefits),
                context(10)
        ).output();

        assertThat(output.recommendations()).isEmpty();
        assertThat(output.noRecommendations())
                .extracting(NoRecommendation::customerKey)
                .containsExactly("CUST-A");
        assertThat(output.noRecommendations().getFirst().reasonCodes())
                .containsExactly("NO_ELIGIBLE_PRODUCT");
    }

    @Test
    void rejectsDistinctCustomerCountAboveLimit() {
        RankRecommendationsTool tool = new RankRecommendationsTool(CLOCK);
        BenefitCalculationResult benefits = result(List.of(
                eligible("CUST-A", "CARD-A", "10000", "120000"),
                eligible("CUST-B", "CARD-B", "10000", "120000")
        ));

        ToolExecutionResult<RecommendationRankingResult> result = tool.execute(
                new RankRecommendationsInput(benefits),
                context(1)
        );

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.REJECTED);
        assertThat(result.reasonCodes()).containsExactly("RESULT_LIMIT_EXCEEDED");
        assertThat(result.output()).isNull();
    }

    private static BenefitCalculationResult result(List<ExpectedBenefit> benefits) {
        return new BenefitCalculationResult("KRW", 12, benefits);
    }

    private static ExpectedBenefit eligible(
            String customerKey,
            String productId,
            String monthlyBenefit,
            String annualBenefit
    ) {
        return new ExpectedBenefit(
                customerKey,
                productId,
                true,
                new BigDecimal(monthlyBenefit),
                new BigDecimal(annualBenefit),
                List.of("PREVIOUS_MONTH_SPEND_MET")
        );
    }

    private static ExpectedBenefit ineligible(String customerKey, String productId) {
        return new ExpectedBenefit(
                customerKey,
                productId,
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of("PREVIOUS_MONTH_SPEND_NOT_MET")
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
