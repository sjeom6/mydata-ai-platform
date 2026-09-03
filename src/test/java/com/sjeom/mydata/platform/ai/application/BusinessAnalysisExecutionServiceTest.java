package com.sjeom.mydata.platform.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sjeom.mydata.platform.ai.llm.LlmClient;
import com.sjeom.mydata.platform.ai.llm.LlmPlanRequest;
import com.sjeom.mydata.platform.analysis.application.AnalysisPlanExecutionFacade;
import com.sjeom.mydata.platform.analysis.application.AnalysisRequestResult;
import com.sjeom.mydata.platform.analysis.application.AnalysisRequestStatus;
import com.sjeom.mydata.platform.analysis.input.AnalysisPlanJsonReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BusinessAnalysisExecutionServiceTest {

    @Test
    void retriesOnceWithValidationErrorsWhenModelRequestsForbiddenTool() {
        List<LlmPlanRequest> requests = new ArrayList<>();
        String validPlan = validPlanJson();
        LlmClient client = request -> {
            requests.add(request);
            return request.attempt() == 1
                    ? validPlan.replace("GET_CONSUMPTION_SUMMARY", "EXPORT_ALL_CUSTOMERS")
                    : validPlan;
        };
        AnalysisPlanExecutionFacade facade = mock(AnalysisPlanExecutionFacade.class);
        AnalysisRequestResult expected = new AnalysisRequestResult(
                UUID.randomUUID(), AnalysisRequestStatus.SUCCESS, "PLAN-LLM-TEST",
                null, List.of(), List.of()
        );
        when(facade.execute(any(), any(), any(), any(), any(Integer.class)))
                .thenReturn(expected);

        BusinessAnalysisExecutionService service = new BusinessAnalysisExecutionService(
                client, new AnalysisPlanJsonReader(), facade
        );

        AnalysisRequestResult actual = service.execute(
                "카페 고객에게 카드 추천", "requester", "CARD_RECOMMENDATION",
                LocalDate.of(2026, 9, 3), 100
        );

        assertThat(actual).isSameAs(expected);
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).validationErrors()).isEmpty();
        assertThat(requests.get(1).validationErrors()).isNotEmpty();
        verify(facade).execute(
                validPlan, "requester", "CARD_RECOMMENDATION",
                LocalDate.of(2026, 9, 3), 100
        );
    }

    private static String validPlanJson() {
        return """
                {
                  "planId": "PLAN-LLM-TEST",
                  "planVersion": "1.0",
                  "segmentCode": "COFFEE_HEAVY_USER",
                  "period": { "type": "RELATIVE_MONTH", "value": 3 },
                  "conditions": [{
                    "category": "CAFE",
                    "metric": "MONTHLY_AVG_AMOUNT",
                    "operator": "GTE",
                    "value": 100000,
                    "currency": "KRW"
                  }],
                  "toolSteps": [
                    "GET_CONSUMPTION_SUMMARY",
                    "FILTER_CUSTOMER_SEGMENT",
                    "SEARCH_CARD_PRODUCTS",
                    "CALCULATE_EXPECTED_BENEFIT",
                    "RANK_RECOMMENDATIONS"
                  ],
                  "productMatching": {
                    "productType": "CREDIT_CARD",
                    "benefitCategory": "CAFE",
                    "rankingMetric": "EXPECTED_ANNUAL_BENEFIT"
                  },
                  "policyStatus": "CURRENTLY_ALLOWED"
                }
                """;
    }
}
