package com.sjeom.mydata.platform.analysis.application;

import com.sjeom.mydata.platform.tool.recommendation.RecommendationRankingResult;
import java.util.List;

public record AnalysisExecutionResult(
        String analysisPlanId,
        AnalysisExecutionStatus status,
        RecommendationRankingResult output,
        String failedStep,
        List<String> reasonCodes
) {

    public AnalysisExecutionResult {
        reasonCodes = List.copyOf(reasonCodes);
    }

    public static AnalysisExecutionResult success(
            String analysisPlanId,
            RecommendationRankingResult output
    ) {
        return new AnalysisExecutionResult(
                analysisPlanId,
                AnalysisExecutionStatus.SUCCESS,
                output,
                null,
                List.of()
        );
    }

    public static AnalysisExecutionResult rejected(
            String analysisPlanId,
            String failedStep,
            List<String> reasonCodes
    ) {
        return new AnalysisExecutionResult(
                analysisPlanId,
                AnalysisExecutionStatus.REJECTED,
                null,
                failedStep,
                reasonCodes
        );
    }
}
