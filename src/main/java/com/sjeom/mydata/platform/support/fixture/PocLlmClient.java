package com.sjeom.mydata.platform.support.fixture;

import com.sjeom.mydata.platform.ai.llm.LlmClient;
import com.sjeom.mydata.platform.ai.llm.LlmPlanRequest;

public final class PocLlmClient implements LlmClient {

    @Override
    public String generateAnalysisPlan(LlmPlanRequest request) {
        boolean travelRequest = request.businessRequest().contains("여행")
                || request.businessRequest().contains("해외");
        return travelRequest
                ? plan("PLAN-POC-LLM-TRAVEL", "TRAVEL_POTENTIAL_CUSTOMER", "TRAVEL", "250000")
                : plan("PLAN-POC-LLM-COFFEE", "COFFEE_HEAVY_USER", "CAFE", "100000");
    }

    private static String plan(
            String planId,
            String segmentCode,
            String category,
            String threshold
    ) {
        return """
                {
                  "planId": "%s",
                  "planVersion": "1.0",
                  "segmentCode": "%s",
                  "period": { "type": "RELATIVE_MONTH", "value": 3 },
                  "conditions": [{
                    "category": "%s",
                    "metric": "MONTHLY_AVG_AMOUNT",
                    "operator": "GTE",
                    "value": %s,
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
                    "benefitCategory": "%s",
                    "rankingMetric": "EXPECTED_ANNUAL_BENEFIT"
                  },
                  "policyStatus": "CURRENTLY_ALLOWED"
                }
                """.formatted(planId, segmentCode, category, threshold, category);
    }
}
