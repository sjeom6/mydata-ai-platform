package com.sjeom.mydata.platform.support.fixture;

import com.sjeom.mydata.platform.ai.llm.LlmClient;
import com.sjeom.mydata.platform.ai.llm.LlmPlanRequest;

public final class PocLlmClient implements LlmClient {

    @Override
    public String generateAnalysisPlan(LlmPlanRequest request) {
        return """
                {
                  "planId": "PLAN-POC-LLM-COFFEE",
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
