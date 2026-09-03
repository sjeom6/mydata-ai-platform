package com.sjeom.mydata.platform.analysis.application;

import com.sjeom.mydata.platform.analysis.input.AnalysisPlanInputError;
import com.sjeom.mydata.platform.tool.recommendation.RecommendationRankingResult;
import java.util.List;
import java.util.UUID;

public record AnalysisRequestResult(
        UUID executionId,
        AnalysisRequestStatus status,
        String analysisPlanId,
        RecommendationRankingResult output,
        List<AnalysisPlanInputError> inputErrors,
        List<String> reasonCodes
) {

    public AnalysisRequestResult {
        inputErrors = List.copyOf(inputErrors);
        reasonCodes = List.copyOf(reasonCodes);
    }
}
