package com.sjeom.mydata.platform.tool.benefit;

import com.sjeom.mydata.platform.product.domain.ExpectedBenefit;
import com.sjeom.mydata.platform.product.domain.ProductCandidate;
import com.sjeom.mydata.platform.tool.domain.AiDataTool;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionResult;
import com.sjeom.mydata.platform.tool.segment.CustomerSegmentMember;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class CalculateExpectedBenefitTool
        implements AiDataTool<CalculateExpectedBenefitInput, BenefitCalculationResult> {

    public static final String NAME = "CALCULATE_EXPECTED_BENEFIT";
    public static final int PROJECTION_MONTHS = 12;

    private static final BigDecimal ZERO_KRW = BigDecimal.ZERO.setScale(0, RoundingMode.DOWN);

    private final Clock clock;

    public CalculateExpectedBenefitTool(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Class<CalculateExpectedBenefitInput> inputType() {
        return CalculateExpectedBenefitInput.class;
    }

    @Override
    public Class<BenefitCalculationResult> outputType() {
        return BenefitCalculationResult.class;
    }

    @Override
    public ToolExecutionResult<BenefitCalculationResult> execute(
            CalculateExpectedBenefitInput input,
            ToolExecutionContext context
    ) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(context, "context must not be null");

        if (input.customerSegment().category() != input.productSearchResult().benefitCategory()) {
            return ToolExecutionResult.rejected(NAME, "CATEGORY_MISMATCH", clock.instant());
        }

        long combinationCount = (long) input.customerSegment().members().size()
                * input.productSearchResult().candidates().size();
        if (combinationCount > context.maxResultCount()) {
            return ToolExecutionResult.rejected(NAME, "RESULT_LIMIT_EXCEEDED", clock.instant());
        }

        List<ExpectedBenefit> benefits = new ArrayList<>();
        for (CustomerSegmentMember customer : input.customerSegment().members()) {
            BigDecimal previousMonthSpend = input.previousMonthSpendByCustomer()
                    .getOrDefault(customer.customerKey(), BigDecimal.ZERO);
            for (ProductCandidate product : input.productSearchResult().candidates()) {
                benefits.add(calculate(customer, product, previousMonthSpend));
            }
        }
        benefits.sort(Comparator
                .comparing(ExpectedBenefit::customerKey)
                .thenComparing(ExpectedBenefit::productId));

        BenefitCalculationResult result = new BenefitCalculationResult(
                "KRW",
                PROJECTION_MONTHS,
                benefits
        );
        return ToolExecutionResult.success(NAME, result, clock.instant());
    }

    private static ExpectedBenefit calculate(
            CustomerSegmentMember customer,
            ProductCandidate product,
            BigDecimal previousMonthSpend
    ) {
        if (previousMonthSpend.compareTo(product.minimumPreviousMonthSpend()) < 0) {
            return new ExpectedBenefit(
                    customer.customerKey(),
                    product.productId(),
                    false,
                    ZERO_KRW,
                    ZERO_KRW,
                    List.of("PREVIOUS_MONTH_SPEND_NOT_MET")
            );
        }

        BigDecimal benefitBeforeLimit = customer.monthlyAverageAmount()
                .multiply(product.discountRate())
                .setScale(0, RoundingMode.DOWN);
        BigDecimal monthlyBenefit = benefitBeforeLimit.min(product.monthlyDiscountLimit())
                .setScale(0, RoundingMode.DOWN);
        BigDecimal annualBenefit = monthlyBenefit
                .multiply(BigDecimal.valueOf(PROJECTION_MONTHS))
                .setScale(0, RoundingMode.DOWN);

        List<String> reasonCodes = new ArrayList<>();
        reasonCodes.add("PREVIOUS_MONTH_SPEND_MET");
        if (benefitBeforeLimit.compareTo(product.monthlyDiscountLimit()) > 0) {
            reasonCodes.add("MONTHLY_DISCOUNT_LIMIT_APPLIED");
        }

        return new ExpectedBenefit(
                customer.customerKey(),
                product.productId(),
                true,
                monthlyBenefit,
                annualBenefit,
                reasonCodes
        );
    }
}
