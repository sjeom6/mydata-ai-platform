package com.sjeom.mydata.platform.tool.consumption;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class GetConsumptionSummaryToolTest {

    private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void aggregatesApprovedTransactionsAndSubtractsCancellation() {
        List<CardTransaction> transactions = List.of(
                approved("T-1", "CUST-B", "2026-07-10", ConsumptionCategory.CAFE, "30000"),
                approved("T-2", "CUST-A", "2026-07-15", ConsumptionCategory.CAFE, "60000"),
                approved("T-3", "CUST-A", "2026-08-15", ConsumptionCategory.CAFE, "70000"),
                approved("T-4", "CUST-A", "2026-09-01", ConsumptionCategory.CAFE, "50000"),
                cancelled("T-5", "T-4", "CUST-A", "2026-09-02", ConsumptionCategory.CAFE, "50000"),
                approved("T-6", "CUST-A", "2026-09-02", ConsumptionCategory.DINING, "90000"),
                approved("T-7", "CUST-A", "2026-06-30", ConsumptionCategory.CAFE, "100000")
        );
        GetConsumptionSummaryTool tool = new GetConsumptionSummaryTool(
                (start, end, category) -> transactions,
                CLOCK
        );

        ToolExecutionResult<ConsumptionSummary> result = tool.execute(
                new ConsumptionSummaryInput(3, ConsumptionCategory.CAFE),
                context(10)
        );

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
        assertThat(result.executedAt()).isEqualTo(NOW);
        assertThat(result.output().periodStart()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(result.output().periodEnd()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(result.output().customers())
                .extracting(CustomerConsumptionSummary::customerKey)
                .containsExactly("CUST-A", "CUST-B");

        CustomerConsumptionSummary customerA = result.output().customers().getFirst();
        assertThat(customerA.totalAmount()).isEqualByComparingTo("130000.00");
        assertThat(customerA.monthlyAverageAmount()).isEqualByComparingTo("43333.33");
        assertThat(customerA.approvedTransactionCount()).isEqualTo(3);
    }

    @Test
    void rejectsPeriodLongerThanTwelveMonthsWithoutReadingData() {
        AtomicBoolean repositoryCalled = new AtomicBoolean();
        GetConsumptionSummaryTool tool = new GetConsumptionSummaryTool((start, end, category) -> {
            repositoryCalled.set(true);
            return List.of();
        }, CLOCK);

        ToolExecutionResult<ConsumptionSummary> result = tool.execute(
                new ConsumptionSummaryInput(13, ConsumptionCategory.CAFE),
                context(10)
        );

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.REJECTED);
        assertThat(result.reasonCodes()).containsExactly("PERIOD_LIMIT_EXCEEDED");
        assertThat(repositoryCalled).isFalse();
    }

    @Test
    void rejectsCategoryOutsideAllowlist() {
        GetConsumptionSummaryTool tool = new GetConsumptionSummaryTool(
                (start, end, category) -> List.of(),
                CLOCK
        );

        ToolExecutionResult<ConsumptionSummary> result = tool.execute(
                new ConsumptionSummaryInput(3, ConsumptionCategory.TRAVEL),
                context(10)
        );

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.REJECTED);
        assertThat(result.reasonCodes()).containsExactly("CATEGORY_NOT_ALLOWED");
    }

    @Test
    void rejectsResultThatExceedsContextLimit() {
        List<CardTransaction> transactions = List.of(
                approved("T-1", "CUST-A", "2026-09-01", ConsumptionCategory.CAFE, "10000"),
                approved("T-2", "CUST-B", "2026-09-01", ConsumptionCategory.CAFE, "20000")
        );
        GetConsumptionSummaryTool tool = new GetConsumptionSummaryTool(
                (start, end, category) -> transactions,
                CLOCK
        );

        ToolExecutionResult<ConsumptionSummary> result = tool.execute(
                new ConsumptionSummaryInput(3, ConsumptionCategory.CAFE),
                context(1)
        );

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.REJECTED);
        assertThat(result.reasonCodes()).containsExactly("RESULT_LIMIT_EXCEEDED");
        assertThat(result.output()).isNull();
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

    private static CardTransaction approved(
            String transactionId,
            String customerKey,
            String occurredOn,
            ConsumptionCategory category,
            String amount
    ) {
        return new CardTransaction(
                transactionId,
                customerKey,
                LocalDate.parse(occurredOn),
                category,
                new BigDecimal(amount),
                TransactionStatus.APPROVED,
                null
        );
    }

    private static CardTransaction cancelled(
            String transactionId,
            String originalTransactionId,
            String customerKey,
            String occurredOn,
            ConsumptionCategory category,
            String amount
    ) {
        return new CardTransaction(
                transactionId,
                customerKey,
                LocalDate.parse(occurredOn),
                category,
                new BigDecimal(amount),
                TransactionStatus.CANCELLED,
                originalTransactionId
        );
    }
}
