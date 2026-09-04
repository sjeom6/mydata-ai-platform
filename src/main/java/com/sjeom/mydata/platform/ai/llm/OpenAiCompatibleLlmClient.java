package com.sjeom.mydata.platform.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class OpenAiCompatibleLlmClient implements LlmClient {

    private static final String SYSTEM_PROMPT = """
            Convert the business request into one JSON AnalysisPlan object only.
            Never emit SQL, prose, markdown, customer identifiers, or extra fields.
            Allowed categories: CAFE, TRAVEL.
            Use planVersion 1.0, RELATIVE_MONTH 1..12, MONTHLY_AVG_AMOUNT, GTE, KRW,
            CREDIT_CARD, EXPECTED_ANNUAL_BENEFIT, and CURRENTLY_ALLOWED.
            toolSteps must be exactly GET_CONSUMPTION_SUMMARY, FILTER_CUSTOMER_SEGMENT,
            SEARCH_CARD_PRODUCTS, CALCULATE_EXPECTED_BENEFIT, RANK_RECOMMENDATIONS in that order.
            """;

    private final RestClient restClient;
    private final PrivateLlmProperties properties;

    public OpenAiCompatibleLlmClient(
            RestClient.Builder restClientBuilder,
            PrivateLlmProperties properties
    ) {
        this.properties = Objects.requireNonNull(properties);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = Objects.requireNonNull(restClientBuilder)
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    OpenAiCompatibleLlmClient(RestClient restClient, PrivateLlmProperties properties) {
        this.properties = Objects.requireNonNull(properties);
        this.restClient = Objects.requireNonNull(restClient);
    }

    @Override
    public String generateAnalysisPlan(LlmPlanRequest request) {
        Objects.requireNonNull(request);
        try {
            JsonNode response = restClient.post()
                    .uri(properties.path())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(this::applyAuthorization)
                    .body(requestBody(request))
                    .retrieve()
                    .body(JsonNode.class);
            return extractContent(response);
        } catch (RestClientException exception) {
            throw new LlmClientException("Private LLM request failed", exception);
        }
    }

    private Map<String, Object> requestBody(LlmPlanRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("temperature", 0);
        body.put("max_tokens", properties.maxOutputTokens());
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT + AnalysisPlanPrompt.EXACT_TEMPLATE),
                Map.of("role", "user", "content", userPrompt(request))
        ));
        return body;
    }

    private static String userPrompt(LlmPlanRequest request) {
        List<String> lines = new ArrayList<>();
        lines.add("Business request: " + request.businessRequest());
        if (!request.validationErrors().isEmpty()) {
            lines.add("Previous output validation errors: "
                    + String.join(", ", request.validationErrors()));
            lines.add("Correct every validation error in the next JSON object.");
        }
        return String.join("\n", lines);
    }

    private void applyAuthorization(HttpHeaders headers) {
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            headers.setBearerAuth(properties.apiKey());
        }
    }

    private static String extractContent(JsonNode response) {
        if (response == null) {
            throw new LlmClientException("Private LLM returned an empty response");
        }
        String content = response.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText(null);
        if (content == null || content.isBlank()) {
            throw new LlmClientException("Private LLM response did not contain JSON content");
        }
        return content;
    }
}
