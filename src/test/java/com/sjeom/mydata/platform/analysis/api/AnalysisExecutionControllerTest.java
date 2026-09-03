package com.sjeom.mydata.platform.analysis.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("poc")
class AnalysisExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void executesValidatedPlanAndReturnsRankedRecommendations() throws Exception {
        mockMvc.perform(post("/api/v1/analysis-plans/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Requester-Id", "business-user")
                        .header("X-Business-Purpose", "CARD_RECOMMENDATION")
                        .header("X-Data-As-Of", "2026-09-03")
                        .header("X-Max-Result-Count", "100")
                        .content(validPlanJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.analysisPlanId").value("PLAN-20260903-API"))
                .andExpect(jsonPath("$.executionId").isNotEmpty())
                .andExpect(jsonPath("$.result.rankingMetric").value("EXPECTED_ANNUAL_BENEFIT"))
                .andExpect(jsonPath("$.result.recommendations.length()").value(2))
                .andExpect(jsonPath("$.result.recommendations[0].customerKey").value("CUST-ANON-A"))
                .andExpect(jsonPath("$.result.recommendations[0].productId").value("CARD-A"))
                .andExpect(jsonPath("$.result.recommendations[0].annualExpectedBenefit").value(120000))
                .andExpect(jsonPath("$.result.recommendations[1].customerKey").value("CUST-ANON-B"))
                .andExpect(jsonPath("$.result.recommendations[1].productId").value("CARD-B"))
                .andExpect(jsonPath("$.result.recommendations[1].annualExpectedBenefit").value(66000));
    }

    @Test
    void rejectsPlanWithUnknownProperty() throws Exception {
        String invalidJson = validPlanJson().replace(
                "\"policyStatus\": \"CURRENTLY_ALLOWED\"",
                "\"unknown\": true, \"policyStatus\": \"CURRENTLY_ALLOWED\""
        );

        mockMvc.perform(post("/api/v1/analysis-plans/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Requester-Id", "business-user")
                        .header("X-Business-Purpose", "CARD_RECOMMENDATION")
                        .header("X-Data-As-Of", "2026-09-03")
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_PLAN"))
                .andExpect(jsonPath("$.result").doesNotExist())
                .andExpect(jsonPath("$.inputErrors[0].code").value("JSON_SCHEMA_ADDITIONALPROPERTIES"));
    }

    @Test
    void rejectsRequestWithoutRequesterHeader() throws Exception {
        mockMvc.perform(post("/api/v1/analysis-plans/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Business-Purpose", "CARD_RECOMMENDATION")
                        .header("X-Data-As-Of", "2026-09-03")
                        .content(validPlanJson()))
                .andExpect(status().isBadRequest());
    }

    private static String validPlanJson() {
        return """
                {
                  "planId": "PLAN-20260903-API",
                  "planVersion": "1.0",
                  "segmentCode": "COFFEE_HEAVY_USER",
                  "period": { "type": "RELATIVE_MONTH", "value": 3 },
                  "conditions": [
                    {
                      "category": "CAFE",
                      "metric": "MONTHLY_AVG_AMOUNT",
                      "operator": "GTE",
                      "value": 100000,
                      "currency": "KRW"
                    }
                  ],
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
