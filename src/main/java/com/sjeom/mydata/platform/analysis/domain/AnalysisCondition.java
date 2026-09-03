package com.sjeom.mydata.platform.analysis.domain;

import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import java.math.BigDecimal;

public record AnalysisCondition(
        ConsumptionCategory category,
        ConditionMetric metric,
        ComparisonOperator operator,
        BigDecimal value,
        String currency
) {

    public AnalysisCondition {
        if (category == null || metric == null || operator == null) {
            throw new IllegalArgumentException("condition category, metric and operator must not be null");
        }
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("condition value must not be negative");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
    }
}
