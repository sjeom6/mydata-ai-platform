package com.sjeom.mydata.platform.tool.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ToolExecutionContextTest {

    @Test
    void rejectsNonPositiveResultLimit() {
        assertThatThrownBy(() -> new ToolExecutionContext(
                UUID.randomUUID(),
                "business-user",
                "CARD_RECOMMENDATION",
                Instant.parse("2026-09-03T00:00:00Z"),
                LocalDate.of(2026, 9, 3),
                0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxResultCount must be positive");
    }

    @Test
    void rejectsBlankPurpose() {
        assertThatThrownBy(() -> new ToolExecutionContext(
                UUID.randomUUID(),
                "business-user",
                " ",
                Instant.parse("2026-09-03T00:00:00Z"),
                LocalDate.of(2026, 9, 3),
                100
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("purpose must not be blank");
    }
}
