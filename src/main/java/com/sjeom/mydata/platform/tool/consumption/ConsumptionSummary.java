package com.sjeom.mydata.platform.tool.consumption;

import java.time.LocalDate;
import java.util.List;

public record ConsumptionSummary(
        ConsumptionCategory category,
        LocalDate periodStart,
        LocalDate periodEnd,
        int months,
        List<CustomerConsumptionSummary> customers
) {

    public ConsumptionSummary {
        customers = List.copyOf(customers);
    }
}
