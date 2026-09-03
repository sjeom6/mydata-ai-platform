package com.sjeom.mydata.platform.tool.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ToolExecutionResultTest {

    private static final Instant EXECUTED_AT = Instant.parse("2026-09-03T00:00:00Z");

    @Test
    void createsSuccessfulResultWithOutput() {
        ToolExecutionResult<String> result = ToolExecutionResult.success(
                "GET_CONSUMPTION_SUMMARY",
                "summary",
                EXECUTED_AT
        );

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
        assertThat(result.output()).isEqualTo("summary");
        assertThat(result.reasonCodes()).isEmpty();
    }

    @Test
    void createsRejectedResultWithReasonCode() {
        ToolExecutionResult<String> result = ToolExecutionResult.rejected(
                "GET_CONSUMPTION_SUMMARY",
                "PERIOD_LIMIT_EXCEEDED",
                EXECUTED_AT
        );

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.REJECTED);
        assertThat(result.output()).isNull();
        assertThat(result.reasonCodes()).containsExactly("PERIOD_LIMIT_EXCEEDED");
    }

    @Test
    void rejectsSuccessWithoutOutput() {
        assertThatThrownBy(() -> ToolExecutionResult.success(
                "GET_CONSUMPTION_SUMMARY",
                null,
                EXECUTED_AT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("successful result must contain output");
    }
}
