package com.sjeom.mydata.platform.analysis.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ApiSafetyAndReproducibilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rejectsExcessiveResultLimitWithSanitizedError() throws Exception {
        mockMvc.perform(post("/api/v1/business-analysis/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Requester-Id", "business-user")
                        .header("X-Business-Purpose", "CARD_RECOMMENDATION")
                        .header("X-Data-As-Of", "2026-09-03")
                        .header("X-Max-Result-Count", "1001")
                        .content("{\"request\":\"카페 고객 카드 추천\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Request headers or body are invalid"));
    }

    @Test
    void producesSameBusinessResultForSameRequestAndDataAsOf() throws Exception {
        String first = executeNaturalLanguageRequest();
        String second = executeNaturalLanguageRequest();

        JsonNode firstResult = objectMapper.readTree(first).get("result");
        JsonNode secondResult = objectMapper.readTree(second).get("result");

        assertThat(firstResult).isEqualTo(secondResult);
    }

    private String executeNaturalLanguageRequest() throws Exception {
        return mockMvc.perform(post("/api/v1/business-analysis/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Requester-Id", "business-user")
                        .header("X-Business-Purpose", "CARD_RECOMMENDATION")
                        .header("X-Data-As-Of", "2026-09-03")
                        .content("{\"request\":\"카페 고객 카드 추천\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
