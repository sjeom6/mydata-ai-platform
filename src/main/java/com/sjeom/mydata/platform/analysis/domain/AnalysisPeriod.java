package com.sjeom.mydata.platform.analysis.domain;

public record AnalysisPeriod(PeriodType type, int value) {

    public AnalysisPeriod {
        if (type == null) {
            throw new IllegalArgumentException("period type must not be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("period value must be positive");
        }
    }
}
