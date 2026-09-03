package com.sjeom.mydata.platform.analysis.api;

import com.sjeom.mydata.platform.analysis.application.AnalysisRequestResult;
import com.sjeom.mydata.platform.analysis.input.AnalysisPlanInputError;
import com.sjeom.mydata.platform.tool.recommendation.RecommendationRankingResult;
import java.util.List;
import java.util.UUID;

public record AnalysisExecutionApiResponse(
        UUID executionId,
        String status,
        String analysisPlanId,
        RecommendationRankingResult result,
        List<AnalysisPlanInputError> inputErrors,
        List<String> reasonCodes
) {

    public static AnalysisExecutionApiResponse from(AnalysisRequestResult source) {
        return new AnalysisExecutionApiResponse(
                source.executionId(),
                source.status().name(),
                source.analysisPlanId(),
                source.output(),
                source.inputErrors(),
                source.reasonCodes()
        );
    }
}
