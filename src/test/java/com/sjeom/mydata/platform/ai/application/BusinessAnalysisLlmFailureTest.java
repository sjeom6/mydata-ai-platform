package com.sjeom.mydata.platform.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.sjeom.mydata.platform.ai.llm.LlmClient;
import com.sjeom.mydata.platform.analysis.application.AnalysisPlanExecutionFacade;
import com.sjeom.mydata.platform.analysis.application.AnalysisRequestResult;
import com.sjeom.mydata.platform.analysis.application.AnalysisRequestStatus;
import com.sjeom.mydata.platform.analysis.input.AnalysisPlanJsonReader;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BusinessAnalysisLlmFailureTest {

    @Test
    void returnsSanitizedFailureWhenLlmCallFails() {
        LlmClient client = request -> {
            throw new IllegalStateException("private endpoint and credential details");
        };
        BusinessAnalysisExecutionService service = new BusinessAnalysisExecutionService(
                client,
                new AnalysisPlanJsonReader(),
                mock(AnalysisPlanExecutionFacade.class)
        );

        AnalysisRequestResult result = service.execute(
                "카페 고객 카드 추천",
                "requester",
                "CARD_RECOMMENDATION",
                LocalDate.of(2026, 9, 3),
                100
        );

        assertThat(result.status()).isEqualTo(AnalysisRequestStatus.FAILED);
        assertThat(result.reasonCodes()).containsExactly("LLM_GENERATION_FAILED");
        assertThat(result.reasonCodes().toString()).doesNotContain("credential");
    }
}
