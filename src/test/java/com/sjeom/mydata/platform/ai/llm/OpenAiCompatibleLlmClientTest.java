package com.sjeom.mydata.platform.ai.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiCompatibleLlmClientTest {

    @Test
    void sendsAuthenticatedJsonRequestAndExtractsPlanContent() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://private-llm.internal");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://private-llm.internal/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-secret"))
                .andExpect(jsonPath("$.max_tokens").value(4096))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "\\\"planId\\\": \\\"PLAN-LLM-CAFE\\\""
                )))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Previous output validation errors: JSON_SCHEMA_CONST@/toolSteps/0"
                )))
                .andRespond(withSuccess("""
                        {
                          "choices": [{
                            "message": {"content": "{\\"planId\\":\\"PLAN-PRIVATE-1\\"}"}
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));
        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(
                builder.build(), properties("test-secret")
        );

        String plan = client.generateAnalysisPlan(new LlmPlanRequest(
                "여행 고객 카드 추천",
                2,
                List.of("JSON_SCHEMA_CONST@/toolSteps/0")
        ));

        assertThat(plan).isEqualTo("{\"planId\":\"PLAN-PRIVATE-1\"}");
        server.verify();
    }

    @Test
    void convertsHttpFailureToLlmClientExceptionWithoutResponseDetails() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://private-llm.internal");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://private-llm.internal/v1/chat/completions"))
                .andRespond(withServerError().body("internal model topology"));
        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(
                builder.build(), properties("")
        );

        assertThatThrownBy(() -> client.generateAnalysisPlan(
                new LlmPlanRequest("카페 고객 카드 추천", 1, List.of())
        ))
                .isInstanceOf(LlmClientException.class)
                .hasMessage("Private LLM request failed")
                .hasMessageNotContaining("topology");
        server.verify();
    }

    private static PrivateLlmProperties properties(String apiKey) {
        return new PrivateLlmProperties(
                "http://private-llm.internal",
                "/v1/chat/completions",
                "internal-model",
                apiKey,
                Duration.ofSeconds(3),
                Duration.ofSeconds(30),
                4096
        );
    }
}
