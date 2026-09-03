package com.sjeom.mydata.platform.ai.api;

import jakarta.validation.constraints.NotBlank;

public record BusinessAnalysisApiRequest(@NotBlank String request) {
}
