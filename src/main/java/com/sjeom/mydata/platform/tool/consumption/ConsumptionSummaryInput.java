package com.sjeom.mydata.platform.tool.consumption;

public record ConsumptionSummaryInput(
        int months,
        ConsumptionCategory category
) {

    public ConsumptionSummaryInput {
        if (months <= 0) {
            throw new IllegalArgumentException("months must be positive");
        }
        if (category == null) {
            throw new IllegalArgumentException("category must not be null");
        }
    }
}
