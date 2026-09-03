package com.sjeom.mydata.platform.product.domain;

import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import java.math.BigDecimal;

public record ProductCandidate(
        String productId,
        String name,
        ConsumptionCategory benefitCategory,
        BigDecimal discountRate,
        BigDecimal monthlyDiscountLimit,
        BigDecimal minimumPreviousMonthSpend
) {

    public static ProductCandidate from(CardProduct product) {
        return new ProductCandidate(
                product.productId(),
                product.name(),
                product.benefitCategory(),
                product.discountRate(),
                product.monthlyDiscountLimit(),
                product.minimumPreviousMonthSpend()
        );
    }
}
