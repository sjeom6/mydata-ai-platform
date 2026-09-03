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
class BusinessAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void convertsNaturalLanguageRequestToValidatedPlanAndExecutesIt() throws Exception {
        mockMvc.perform(post("/api/v1/business-analysis/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Requester-Id", "business-user")
                        .header("X-Business-Purpose", "CARD_RECOMMENDATION")
                        .header("X-Data-As-Of", "2026-09-03")
                        .content("""
                                {
                                  "request": "최근 3개월 카페 월평균 10만원 이상 고객에게 카드를 추천해줘"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.analysisPlanId").value("PLAN-POC-LLM-COFFEE"))
                .andExpect(jsonPath("$.result.recommendations.length()").value(2))
                .andExpect(jsonPath("$.result.recommendations[0].customerKey").value("CUST-ANON-A"))
                .andExpect(jsonPath("$.result.recommendations[0].productId").value("CARD-A"));
    }

    @Test
    void rejectsBlankBusinessRequest() throws Exception {
        mockMvc.perform(post("/api/v1/business-analysis/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Requester-Id", "business-user")
                        .header("X-Business-Purpose", "CARD_RECOMMENDATION")
                        .header("X-Data-As-Of", "2026-09-03")
                        .content("{\"request\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
}
