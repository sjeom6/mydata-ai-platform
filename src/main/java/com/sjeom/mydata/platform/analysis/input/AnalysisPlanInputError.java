package com.sjeom.mydata.platform.analysis.input;

public record AnalysisPlanInputError(
        String code,
        String field,
        String message
) {
}
