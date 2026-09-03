package com.sjeom.mydata.platform.tool.recommendation;

import com.sjeom.mydata.platform.product.domain.NoRecommendation;
import com.sjeom.mydata.platform.product.domain.ProductRecommendation;
import java.util.List;

public record RecommendationRankingResult(
        String rankingMetric,
        List<ProductRecommendation> recommendations,
        List<NoRecommendation> noRecommendations
) {

    public RecommendationRankingResult {
        recommendations = List.copyOf(recommendations);
        noRecommendations = List.copyOf(noRecommendations);
    }
}
