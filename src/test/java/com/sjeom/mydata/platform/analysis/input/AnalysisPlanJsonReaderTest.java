package com.sjeom.mydata.platform.analysis.input;

import static org.assertj.core.api.Assertions.assertThat;

import com.sjeom.mydata.platform.analysis.domain.PolicyStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisPlanJsonReaderTest {

    private final AnalysisPlanJsonReader reader = new AnalysisPlanJsonReader();

    @Test
    void readsSchemaAndPolicyValidPlan() {
        AnalysisPlanInputResult result = reader.read(validJson());

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.plan().planId()).isEqualTo("PLAN-20260903-0001");
        assertThat(result.plan().policyStatus()).isEqualTo(PolicyStatus.CURRENTLY_ALLOWED);
    }

    @Test
    void rejectsUnknownJsonProperty() {
        String json = validJson().replace(
                "\"policyStatus\": \"CURRENTLY_ALLOWED\"",
                "\"unexpectedField\": true,\n  \"policyStatus\": \"CURRENTLY_ALLOWED\""
        );

        AnalysisPlanInputResult result = reader.read(json);

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.plan()).isNull();
        assertThat(result.errors())
                .extracting(AnalysisPlanInputError::code)
                .contains("JSON_SCHEMA_ADDITIONALPROPERTIES");
    }

    @Test
    void rejectsPeriodAboveSchemaMaximum() {
        AnalysisPlanInputResult result = reader.read(validJson().replace(
                "\"value\": 3",
                "\"value\": 13"
        ));

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.errors())
                .extracting(AnalysisPlanInputError::code)
                .contains("JSON_SCHEMA_MAXIMUM");
    }

    @Test
    void rejectsReviewRequiredPlanInSemanticValidation() {
        AnalysisPlanInputResult result = reader.read(validJson().replace(
                "CURRENTLY_ALLOWED",
                "REVIEW_REQUIRED"
        ));

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.errors())
                .extracting(AnalysisPlanInputError::code)
                .containsExactly("POLICY_NOT_ALLOWED");
    }

    @Test
    void rejectsMalformedJsonWithoutExposingInput() {
        AnalysisPlanInputResult result = reader.read("{not-json}");

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.errors()).containsExactly(new AnalysisPlanInputError(
                "MALFORMED_JSON",
                "$",
                "JSON input is malformed"
        ));
    }

    private static String validJson() {
        return """
                {
                  "planId": "PLAN-20260903-0001",
                  "planVersion": "1.0",
                  "segmentCode": "COFFEE_HEAVY_USER",
                  "period": {
                    "type": "RELATIVE_MONTH",
                    "value": 3
                  },
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
