package com.sjeom.mydata.platform.support.fixture;

import com.sjeom.mydata.platform.analysis.application.PreviousMonthSpendProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public final class PocPreviousMonthSpendProvider implements PreviousMonthSpendProvider {

    private final Map<String, BigDecimal> previousMonthSpendByCustomer;

    public PocPreviousMonthSpendProvider(Map<String, BigDecimal> previousMonthSpendByCustomer) {
        this.previousMonthSpendByCustomer = Map.copyOf(previousMonthSpendByCustomer);
    }

    @Override
    public Map<String, BigDecimal> findByDataAsOf(LocalDate dataAsOf) {
        return previousMonthSpendByCustomer;
    }
}
