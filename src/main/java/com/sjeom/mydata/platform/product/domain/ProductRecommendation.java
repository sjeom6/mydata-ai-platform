package com.sjeom.mydata.platform.product.domain;

import java.math.BigDecimal;
import java.util.List;

public record ProductRecommendation(
        String customerKey,
        String productId,
        BigDecimal monthlyExpectedBenefit,
        BigDecimal annualExpectedBenefit,
        String currency,
        List<String> reasonCodes
) {

    public ProductRecommendation {
        reasonCodes = List.copyOf(reasonCodes);
    }
}
