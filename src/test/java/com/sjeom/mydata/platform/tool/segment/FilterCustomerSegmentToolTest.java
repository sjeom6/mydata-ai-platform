package com.sjeom.mydata.platform.tool.segment;

import static org.assertj.core.api.Assertions.assertThat;

import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionSummary;
import com.sjeom.mydata.platform.tool.consumption.CustomerConsumptionSummary;
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

class FilterCustomerSegmentToolTest {

    private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void includesBoundaryValueAndExcludesCustomerBelowThreshold() {
        ConsumptionSummary summary = summary(List.of(
                customer("CUST-A", "300000", "100000", 9),
                customer("CUST-B", "299999.97", "99999.99", 12),
                customer("CUST-C", "450000", "150000", 4)
        ));
        FilterCustomerSegmentTool tool = new FilterCustomerSegmentTool(CLOCK);

        ToolExecutionResult<CustomerSegment> result = tool.execute(
                new FilterCustomerSegmentInput(
                        "COFFEE_HEAVY_USER",
                        summary,
                        new BigDecimal("100000")
                ),
                context(10)
        );

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
        assertThat(result.output().members())
                .extracting(CustomerSegmentMember::customerKey)
                .containsExactly("CUST-A", "CUST-C");
        assertThat(result.output().members().getFirst().reasonCodes())
                .containsExactly("CAFE_MONTHLY_AVG_THRESHOLD_MET");
    }

    @Test
    void retainsDeterministicAmountsFromConsumptionSummary() {
        ConsumptionSummary summary = summary(List.of(
                customer("CUST-A", "330000.00", "110000.00", 7)
        ));
        FilterCustomerSegmentTool tool = new FilterCustomerSegmentTool(CLOCK);

        ToolExecutionResult<CustomerSegment> result = tool.execute(
                new FilterCustomerSegmentInput(
                        "COFFEE_HEAVY_USER",
                        summary,
                        new BigDecimal("100000")
                ),
                context(10)
        );

        CustomerSegmentMember member = result.output().members().getFirst();
        assertThat(member.totalAmount()).isEqualByComparingTo("330000.00");
        assertThat(member.monthlyAverageAmount()).isEqualByComparingTo("110000.00");
        assertThat(member.approvedTransactionCount()).isEqualTo(7);
        assertThat(result.executedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsInputLargerThanExecutionLimit() {
        ConsumptionSummary summary = summary(List.of(
                customer("CUST-A", "300000", "100000", 3),
                customer("CUST-B", "600000", "200000", 6)
        ));
        FilterCustomerSegmentTool tool = new FilterCustomerSegmentTool(CLOCK);

        ToolExecutionResult<CustomerSegment> result = tool.execute(
                new FilterCustomerSegmentInput(
                        "COFFEE_HEAVY_USER",
                        summary,
                        new BigDecimal("100000")
                ),
                context(1)
        );

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.REJECTED);
        assertThat(result.reasonCodes()).containsExactly("INPUT_RESULT_LIMIT_EXCEEDED");
        assertThat(result.output()).isNull();
    }

    private static ConsumptionSummary summary(List<CustomerConsumptionSummary> customers) {
        return new ConsumptionSummary(
                ConsumptionCategory.CAFE,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 3),
                3,
                customers
        );
    }

    private static CustomerConsumptionSummary customer(
            String customerKey,
            String totalAmount,
            String monthlyAverageAmount,
            long approvedTransactionCount
    ) {
        return new CustomerConsumptionSummary(
                customerKey,
                new BigDecimal(totalAmount),
                new BigDecimal(monthlyAverageAmount),
                approvedTransactionCount
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
