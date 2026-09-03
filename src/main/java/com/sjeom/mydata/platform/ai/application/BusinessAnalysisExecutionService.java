package com.sjeom.mydata.platform.ai.application;

import com.sjeom.mydata.platform.ai.llm.LlmClient;
import com.sjeom.mydata.platform.ai.llm.LlmPlanRequest;
import com.sjeom.mydata.platform.analysis.application.AnalysisPlanExecutionFacade;
import com.sjeom.mydata.platform.analysis.application.AnalysisRequestResult;
import com.sjeom.mydata.platform.analysis.input.AnalysisPlanInputError;
import com.sjeom.mydata.platform.analysis.input.AnalysisPlanInputResult;
import com.sjeom.mydata.platform.analysis.input.AnalysisPlanJsonReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class BusinessAnalysisExecutionService {

    private static final int MAX_GENERATION_ATTEMPTS = 2;

    private final LlmClient llmClient;
    private final AnalysisPlanJsonReader jsonReader;
    private final AnalysisPlanExecutionFacade executionFacade;

    public BusinessAnalysisExecutionService(
            LlmClient llmClient,
            AnalysisPlanJsonReader jsonReader,
            AnalysisPlanExecutionFacade executionFacade
    ) {
        this.llmClient = Objects.requireNonNull(llmClient);
        this.jsonReader = Objects.requireNonNull(jsonReader);
        this.executionFacade = Objects.requireNonNull(executionFacade);
    }

    public AnalysisRequestResult execute(
            String businessRequest,
            String requesterId,
            String purpose,
            LocalDate dataAsOf,
            int maxResultCount
    ) {
        List<String> validationErrors = List.of();
        String generatedPlan = null;

        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            generatedPlan = llmClient.generateAnalysisPlan(
                    new LlmPlanRequest(businessRequest, attempt, validationErrors)
            );
            AnalysisPlanInputResult input = jsonReader.read(generatedPlan);
            if (input.isAccepted()) {
                return executionFacade.execute(
                        generatedPlan, requesterId, purpose, dataAsOf, maxResultCount
                );
            }
            validationErrors = input.errors().stream()
                    .map(BusinessAnalysisExecutionService::summarize)
                    .toList();
        }

        return executionFacade.execute(
                generatedPlan, requesterId, purpose, dataAsOf, maxResultCount
        );
    }

    private static String summarize(AnalysisPlanInputError error) {
        return error.code() + "@" + error.field();
    }
}
