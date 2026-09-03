package com.sjeom.mydata.platform.product.domain;

import java.util.List;

public record NoRecommendation(
        String customerKey,
        List<String> reasonCodes
) {

    public NoRecommendation {
        reasonCodes = List.copyOf(reasonCodes);
    }
}
