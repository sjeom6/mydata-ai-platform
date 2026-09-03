package com.sjeom.mydata.platform.tool.consumption;

import com.sjeom.mydata.platform.tool.domain.AiDataTool;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GetConsumptionSummaryTool
        implements AiDataTool<ConsumptionSummaryInput, ConsumptionSummary> {

    public static final String NAME = "GET_CONSUMPTION_SUMMARY";
    public static final int MAX_MONTHS = 12;

    private static final Set<ConsumptionCategory> ALLOWED_CATEGORIES = Set.of(ConsumptionCategory.CAFE, ConsumptionCategory.TRAVEL);

    private final ConsumptionTransactionRepository repository;
    private final Clock clock;

    public GetConsumptionSummaryTool(ConsumptionTransactionRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Class<ConsumptionSummaryInput> inputType() {
        return ConsumptionSummaryInput.class;
    }

    @Override
    public Class<ConsumptionSummary> outputType() {
        return ConsumptionSummary.class;
    }

    @Override
    public ToolExecutionResult<ConsumptionSummary> execute(
            ConsumptionSummaryInput input,
            ToolExecutionContext context
    ) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(context, "context must not be null");

        if (input.months() > MAX_MONTHS) {
            return ToolExecutionResult.rejected(NAME, "PERIOD_LIMIT_EXCEEDED", clock.instant());
        }
        if (!ALLOWED_CATEGORIES.contains(input.category())) {
            return ToolExecutionResult.rejected(NAME, "CATEGORY_NOT_ALLOWED", clock.instant());
        }

        LocalDate periodEnd = context.dataAsOf();
        LocalDate periodStart = periodEnd.withDayOfMonth(1).minusMonths(input.months() - 1L);
        List<CardTransaction> transactions = Objects.requireNonNull(
                repository.findByPeriodAndCategory(periodStart, periodEnd, input.category()),
                "repository result must not be null"
        );

        Map<String, CustomerAccumulator> accumulators = new LinkedHashMap<>();
        transactions.stream()
                .filter(transaction -> !transaction.occurredOn().isBefore(periodStart))
                .filter(transaction -> !transaction.occurredOn().isAfter(periodEnd))
                .filter(transaction -> transaction.category() == input.category())
                .forEach(transaction -> accumulators
                        .computeIfAbsent(transaction.customerKey(), ignored -> new CustomerAccumulator())
                        .add(transaction));

        List<CustomerConsumptionSummary> customers = accumulators.entrySet().stream()
                .map(entry -> entry.getValue().toSummary(entry.getKey(), input.months()))
                .sorted(Comparator.comparing(CustomerConsumptionSummary::customerKey))
                .toList();

        if (customers.size() > context.maxResultCount()) {
            return ToolExecutionResult.rejected(NAME, "RESULT_LIMIT_EXCEEDED", clock.instant());
        }

        ConsumptionSummary summary = new ConsumptionSummary(
                input.category(),
                periodStart,
                periodEnd,
                input.months(),
                customers
        );
        return ToolExecutionResult.success(NAME, summary, clock.instant());
    }

    private static final class CustomerAccumulator {

        private BigDecimal totalAmount = BigDecimal.ZERO;
        private long approvedTransactionCount;

        void add(CardTransaction transaction) {
            totalAmount = totalAmount.add(transaction.signedAmount());
            if (transaction.status() == TransactionStatus.APPROVED) {
                approvedTransactionCount++;
            }
        }

        CustomerConsumptionSummary toSummary(String customerKey, int months) {
            BigDecimal normalizedTotal = totalAmount.setScale(2, RoundingMode.HALF_UP);
            BigDecimal monthlyAverage = totalAmount.divide(
                    BigDecimal.valueOf(months),
                    2,
                    RoundingMode.HALF_UP
            );
            return new CustomerConsumptionSummary(
                    customerKey,
                    normalizedTotal,
                    monthlyAverage,
                    approvedTransactionCount
            );
        }
    }
}
