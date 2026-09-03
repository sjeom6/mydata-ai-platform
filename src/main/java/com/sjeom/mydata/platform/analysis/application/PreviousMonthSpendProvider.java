package com.sjeom.mydata.platform.analysis.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface PreviousMonthSpendProvider {

    Map<String, BigDecimal> findByDataAsOf(LocalDate dataAsOf);
}
