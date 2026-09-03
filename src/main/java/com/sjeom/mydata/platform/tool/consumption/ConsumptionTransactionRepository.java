package com.sjeom.mydata.platform.tool.consumption;

import java.time.LocalDate;
import java.util.List;

public interface ConsumptionTransactionRepository {

    List<CardTransaction> findByPeriodAndCategory(
            LocalDate periodStart,
            LocalDate periodEnd,
            ConsumptionCategory category
    );
}
