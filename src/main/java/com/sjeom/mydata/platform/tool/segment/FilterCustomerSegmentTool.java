package com.sjeom.mydata.platform.tool.segment;

import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import com.sjeom.mydata.platform.tool.consumption.CustomerConsumptionSummary;
import com.sjeom.mydata.platform.tool.domain.AiDataTool;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionResult;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class FilterCustomerSegmentTool
        implements AiDataTool<FilterCustomerSegmentInput, CustomerSegment> {

    public static final String NAME = "FILTER_CUSTOMER_SEGMENT";

    private final Clock clock;

    public FilterCustomerSegmentTool(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Class<FilterCustomerSegmentInput> inputType() {
        return FilterCustomerSegmentInput.class;
    }

    @Override
    public Class<CustomerSegment> outputType() {
        return CustomerSegment.class;
    }

    @Override
    public ToolExecutionResult<CustomerSegment> execute(
            FilterCustomerSegmentInput input,
            ToolExecutionContext context
    ) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(context, "context must not be null");

        if (input.consumptionSummary().customers().size() > context.maxResultCount()) {
            return ToolExecutionResult.rejected(NAME, "INPUT_RESULT_LIMIT_EXCEEDED", clock.instant());
        }

        String reasonCode = thresholdReasonCode(input.consumptionSummary().category());
        List<CustomerSegmentMember> members = input.consumptionSummary().customers().stream()
                .filter(customer -> meetsThreshold(customer, input))
                .map(customer -> toMember(customer, reasonCode))
                .toList();

        CustomerSegment segment = new CustomerSegment(
                input.segmentCode(),
                input.consumptionSummary().category(),
                input.minimumMonthlyAverageAmount(),
                members
        );
        return ToolExecutionResult.success(NAME, segment, clock.instant());
    }

    private static boolean meetsThreshold(
            CustomerConsumptionSummary customer,
            FilterCustomerSegmentInput input
    ) {
        return customer.monthlyAverageAmount().compareTo(input.minimumMonthlyAverageAmount()) >= 0;
    }

    private static CustomerSegmentMember toMember(
            CustomerConsumptionSummary customer,
            String reasonCode
    ) {
        return new CustomerSegmentMember(
                customer.customerKey(),
                customer.totalAmount(),
                customer.monthlyAverageAmount(),
                customer.approvedTransactionCount(),
                List.of(reasonCode)
        );
    }

    private static String thresholdReasonCode(ConsumptionCategory category) {
        return category.name() + "_MONTHLY_AVG_THRESHOLD_MET";
    }
}
