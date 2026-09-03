package com.sjeom.mydata.platform.tool.product;

import com.sjeom.mydata.platform.product.domain.ProductCandidate;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import java.time.LocalDate;
import java.util.List;

public record CardProductSearchResult(
        ConsumptionCategory benefitCategory,
        LocalDate dataAsOf,
        List<ProductCandidate> candidates
) {

    public CardProductSearchResult {
        candidates = List.copyOf(candidates);
    }
}
