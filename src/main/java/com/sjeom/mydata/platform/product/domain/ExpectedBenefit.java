package com.sjeom.mydata.platform.product.domain;

import java.math.BigDecimal;
import java.util.List;

public record ExpectedBenefit(
        String customerKey,
        String productId,
        boolean eligible,
        BigDecimal monthlyExpectedBenefit,
        BigDecimal annualExpectedBenefit,
        List<String> reasonCodes
) {

    public ExpectedBenefit {
        reasonCodes = List.copyOf(reasonCodes);
    }
}
