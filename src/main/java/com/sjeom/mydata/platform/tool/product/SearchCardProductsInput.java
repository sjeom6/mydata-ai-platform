package com.sjeom.mydata.platform.tool.product;

import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;

public record SearchCardProductsInput(ConsumptionCategory benefitCategory) {

    public SearchCardProductsInput {
        if (benefitCategory == null) {
            throw new IllegalArgumentException("benefitCategory must not be null");
        }
    }
}
