package com.sjeom.mydata.platform.ai.llm;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "platform.llm")
public record PrivateLlmProperties(
        @NotBlank String baseUrl,
        @NotBlank String path,
        @NotBlank String model,
        String apiKey,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Min(128) @Max(16384) int maxOutputTokens
) {
}
