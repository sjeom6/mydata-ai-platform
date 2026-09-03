package com.sjeom.mydata.platform.analysis.domain;

import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;

public record ProductMatching(
        ProductType productType,
        ConsumptionCategory benefitCategory,
        RankingMetric rankingMetric
) {

    public ProductMatching {
        if (productType == null || benefitCategory == null || rankingMetric == null) {
            throw new IllegalArgumentException("product matching fields must not be null");
        }
    }
}
