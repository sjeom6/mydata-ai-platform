package com.sjeom.mydata.platform.tool.benefit;

import com.sjeom.mydata.platform.product.domain.ExpectedBenefit;
import java.util.List;

public record BenefitCalculationResult(
        String currency,
        int projectionMonths,
        List<ExpectedBenefit> benefits
) {

    public BenefitCalculationResult {
        benefits = List.copyOf(benefits);
    }
}
