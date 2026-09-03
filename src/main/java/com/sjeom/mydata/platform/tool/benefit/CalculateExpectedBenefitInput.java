package com.sjeom.mydata.platform.tool.benefit;

import com.sjeom.mydata.platform.tool.product.CardProductSearchResult;
import com.sjeom.mydata.platform.tool.segment.CustomerSegment;
import java.math.BigDecimal;
import java.util.Map;

public record CalculateExpectedBenefitInput(
        CustomerSegment customerSegment,
        CardProductSearchResult productSearchResult,
        Map<String, BigDecimal> previousMonthSpendByCustomer
) {

    public CalculateExpectedBenefitInput {
        if (customerSegment == null) {
            throw new IllegalArgumentException("customerSegment must not be null");
        }
        if (productSearchResult == null) {
            throw new IllegalArgumentException("productSearchResult must not be null");
        }
        if (previousMonthSpendByCustomer == null) {
            throw new IllegalArgumentException("previousMonthSpendByCustomer must not be null");
        }
        previousMonthSpendByCustomer.forEach((customerKey, amount) -> {
            if (customerKey == null || customerKey.isBlank()) {
                throw new IllegalArgumentException("previous month spend customer key must not be blank");
            }
            if (amount == null || amount.signum() < 0) {
                throw new IllegalArgumentException("previous month spend must not be negative");
            }
        });
        previousMonthSpendByCustomer = Map.copyOf(previousMonthSpendByCustomer);
    }
}
