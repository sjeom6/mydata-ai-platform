package com.sjeom.mydata.platform.tool.recommendation;

import com.sjeom.mydata.platform.tool.benefit.BenefitCalculationResult;

public record RankRecommendationsInput(BenefitCalculationResult benefitCalculationResult) {

    public RankRecommendationsInput {
        if (benefitCalculationResult == null) {
            throw new IllegalArgumentException("benefitCalculationResult must not be null");
        }
    }
}
