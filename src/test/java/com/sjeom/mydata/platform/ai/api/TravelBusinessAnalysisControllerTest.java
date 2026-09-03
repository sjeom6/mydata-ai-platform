package com.sjeom.mydata.platform.ai.api;

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
class TravelBusinessAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reusesExistingToolSequenceForTravelCardRecommendation() throws Exception {
        mockMvc.perform(post("/api/v1/business-analysis/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Requester-Id", "business-user")
                        .header("X-Business-Purpose", "TRAVEL_CARD_RECOMMENDATION")
                        .header("X-Data-As-Of", "2026-09-03")
                        .content("""
                                {
                                  "request": "최근 해외여행 지출이 많은 고객에게 해외결제 특화 카드를 추천해줘"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.analysisPlanId").value("PLAN-POC-LLM-TRAVEL"))
                .andExpect(jsonPath("$.result.recommendations.length()").value(2))
                .andExpect(jsonPath("$.result.recommendations[0].customerKey").value("CUST-ANON-D"))
                .andExpect(jsonPath("$.result.recommendations[0].productId").value("TRAVEL-CARD-A"))
                .andExpect(jsonPath("$.result.recommendations[0].annualExpectedBenefit").value(216000))
                .andExpect(jsonPath("$.result.recommendations[1].customerKey").value("CUST-ANON-E"))
                .andExpect(jsonPath("$.result.recommendations[1].productId").value("TRAVEL-CARD-B"))
                .andExpect(jsonPath("$.result.recommendations[1].annualExpectedBenefit").value(72000));
    }
}
