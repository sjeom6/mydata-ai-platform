package com.sjeom.mydata.platform.analysis.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sjeom.mydata.platform.analysis.domain.AnalysisPlan;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Objects;

public final class AnalysisPlanSnapshotFactory {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AnalysisPlanSnapshotFactory(Clock clock) {
        this.objectMapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build();
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public StoredAnalysisPlan create(AnalysisPlan plan, ToolExecutionContext context) {
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(context, "context must not be null");

        String snapshot = toJson(plan);
        String fingerprintInput = snapshot + "\n" + context.dataAsOf();
        return new StoredAnalysisPlan(
                plan,
                snapshot,
                context.dataAsOf(),
                sha256(fingerprintInput),
                context.requesterId(),
                context.purpose(),
                clock.instant()
        );
    }

    private String toJson(AnalysisPlan plan) {
        try {
            return objectMapper.writeValueAsString(plan);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AnalysisPlan snapshot could not be serialized", exception);
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
