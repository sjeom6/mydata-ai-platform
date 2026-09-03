package com.sjeom.mydata.platform.tool.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.sjeom.mydata.platform.product.domain.CardProduct;
import com.sjeom.mydata.platform.product.domain.ProductCandidate;
import com.sjeom.mydata.platform.product.domain.ProductSaleStatus;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
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

class SearchCardProductsToolTest {

    private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate DATA_AS_OF = LocalDate.of(2026, 9, 3);

    @Test
    void returnsOnlySellableValidAndApprovedProductsInStableOrder() {
        List<CardProduct> products = List.of(
                product("CARD-B", ProductSaleStatus.ON_SALE, "2026-01-01", "2026-12-31", true,
                        ConsumptionCategory.CAFE),
                product("CARD-A", ProductSaleStatus.ON_SALE, "2026-09-03", "2026-09-03", true,
                        ConsumptionCategory.CAFE),
                product("CARD-SUSPENDED", ProductSaleStatus.SUSPENDED, "2026-01-01", "2026-12-31", true,
                        ConsumptionCategory.CAFE),
                product("CARD-EXPIRED", ProductSaleStatus.ON_SALE, "2025-01-01", "2026-09-02", true,
                        ConsumptionCategory.CAFE),
                product("CARD-NOT-APPROVED", ProductSaleStatus.ON_SALE, "2026-01-01", "2026-12-31", false,
                        ConsumptionCategory.CAFE),
                product("CARD-TRAVEL", ProductSaleStatus.ON_SALE, "2026-01-01", "2026-12-31", true,
                        ConsumptionCategory.TRAVEL)
        );
        SearchCardProductsTool tool = new SearchCardProductsTool(category -> products, CLOCK);

        ToolExecutionResult<CardProductSearchResult> result = tool.execute(
                new SearchCardProductsInput(ConsumptionCategory.CAFE),
                context(10)
        );

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
        assertThat(result.output().dataAsOf()).isEqualTo(DATA_AS_OF);
        assertThat(result.output().candidates())
                .extracting(ProductCandidate::productId)
                .containsExactly("CARD-A", "CARD-B");
        assertThat(result.executedAt()).isEqualTo(NOW);
    }

    @Test
    void preservesBenefitRulesForNextCalculationStep() {
        SearchCardProductsTool tool = new SearchCardProductsTool(
                category -> List.of(product(
                        "CARD-A",
                        ProductSaleStatus.ON_SALE,
                        "2026-01-01",
                        "2026-12-31",
                        true,
                        ConsumptionCategory.CAFE
                )),
                CLOCK
        );

        ProductCandidate candidate = tool.execute(
                new SearchCardProductsInput(ConsumptionCategory.CAFE),
                context(10)
        ).output().candidates().getFirst();

        assertThat(candidate.discountRate()).isEqualByComparingTo("0.10");
        assertThat(candidate.monthlyDiscountLimit()).isEqualByComparingTo("10000");
        assertThat(candidate.minimumPreviousMonthSpend()).isEqualByComparingTo("300000");
    }

    @Test
    void rejectsCandidateListAboveExecutionLimit() {
        SearchCardProductsTool tool = new SearchCardProductsTool(
                category -> List.of(
                        product("CARD-A", ProductSaleStatus.ON_SALE, "2026-01-01", "2026-12-31", true,
                                ConsumptionCategory.CAFE),
                        product("CARD-B", ProductSaleStatus.ON_SALE, "2026-01-01", "2026-12-31", true,
                                ConsumptionCategory.CAFE)
                ),
                CLOCK
        );

        ToolExecutionResult<CardProductSearchResult> result = tool.execute(
                new SearchCardProductsInput(ConsumptionCategory.CAFE),
                context(1)
        );

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.REJECTED);
        assertThat(result.reasonCodes()).containsExactly("RESULT_LIMIT_EXCEEDED");
        assertThat(result.output()).isNull();
    }

    private static CardProduct product(
            String productId,
            ProductSaleStatus saleStatus,
            String validFrom,
            String validTo,
            boolean complianceApproved,
            ConsumptionCategory benefitCategory
    ) {
        return new CardProduct(
                productId,
                productId + " name",
                saleStatus,
                LocalDate.parse(validFrom),
                LocalDate.parse(validTo),
                benefitCategory,
                new BigDecimal("0.10"),
                new BigDecimal("10000"),
                new BigDecimal("300000"),
                complianceApproved
        );
    }

    private static ToolExecutionContext context(int maxResultCount) {
        return new ToolExecutionContext(
                UUID.fromString("a12c1de7-ce9f-4a21-b8cc-c66b085d5a8d"),
                "business-user",
                "CARD_RECOMMENDATION",
                NOW,
                DATA_AS_OF,
                maxResultCount
        );
    }
}
