package com.sjeom.mydata.platform.tool.consumption;

import java.math.BigDecimal;

public record CustomerConsumptionSummary(
        String customerKey,
        BigDecimal totalAmount,
        BigDecimal monthlyAverageAmount,
        long approvedTransactionCount
) {
}
