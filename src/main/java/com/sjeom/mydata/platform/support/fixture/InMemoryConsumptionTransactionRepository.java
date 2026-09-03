package com.sjeom.mydata.platform.support.fixture;

import com.sjeom.mydata.platform.tool.consumption.CardTransaction;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionTransactionRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class InMemoryConsumptionTransactionRepository
        implements ConsumptionTransactionRepository {

    private final List<CardTransaction> transactions;

    public InMemoryConsumptionTransactionRepository(List<CardTransaction> transactions) {
        this.transactions = List.copyOf(transactions);
    }

    @Override
    public List<CardTransaction> findByPeriodAndCategory(
            LocalDate periodStart,
            LocalDate periodEnd,
            ConsumptionCategory category
    ) {
        return transactions.stream()
                .filter(transaction -> !transaction.occurredOn().isBefore(periodStart))
                .filter(transaction -> !transaction.occurredOn().isAfter(periodEnd))
                .filter(transaction -> transaction.category() == category)
                .sorted(Comparator.comparing(CardTransaction::transactionId))
                .toList();
    }
}
