package com.sjeom.mydata.platform.tool.segment;

import java.math.BigDecimal;
import java.util.List;

public record CustomerSegmentMember(
        String customerKey,
        BigDecimal totalAmount,
        BigDecimal monthlyAverageAmount,
        long approvedTransactionCount,
        List<String> reasonCodes
) {

    public CustomerSegmentMember {
        reasonCodes = List.copyOf(reasonCodes);
    }
}
