package com.sjeom.mydata.platform.tool.segment;

import com.sjeom.mydata.platform.tool.consumption.ConsumptionSummary;
import java.math.BigDecimal;

public record FilterCustomerSegmentInput(
        String segmentCode,
        ConsumptionSummary consumptionSummary,
        BigDecimal minimumMonthlyAverageAmount
) {

    public FilterCustomerSegmentInput {
        if (segmentCode == null || segmentCode.isBlank()) {
            throw new IllegalArgumentException("segmentCode must not be blank");
        }
        if (consumptionSummary == null) {
            throw new IllegalArgumentException("consumptionSummary must not be null");
        }
        if (minimumMonthlyAverageAmount == null || minimumMonthlyAverageAmount.signum() < 0) {
            throw new IllegalArgumentException("minimumMonthlyAverageAmount must not be negative");
        }
    }
}
