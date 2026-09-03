package com.sjeom.mydata.platform.tool.segment;

import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import java.math.BigDecimal;
import java.util.List;

public record CustomerSegment(
        String segmentCode,
        ConsumptionCategory category,
        BigDecimal minimumMonthlyAverageAmount,
        List<CustomerSegmentMember> members
) {

    public CustomerSegment {
        members = List.copyOf(members);
    }
}
