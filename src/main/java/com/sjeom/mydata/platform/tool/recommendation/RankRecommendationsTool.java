package com.sjeom.mydata.platform.tool.recommendation;

import com.sjeom.mydata.platform.product.domain.ExpectedBenefit;
import com.sjeom.mydata.platform.product.domain.NoRecommendation;
import com.sjeom.mydata.platform.product.domain.ProductRecommendation;
import com.sjeom.mydata.platform.tool.domain.AiDataTool;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionResult;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RankRecommendationsTool
        implements AiDataTool<RankRecommendationsInput, RecommendationRankingResult> {

    public static final String NAME = "RANK_RECOMMENDATIONS";
    public static final String RANKING_METRIC = "EXPECTED_ANNUAL_BENEFIT";

    private static final Comparator<ExpectedBenefit> BENEFIT_ORDER = Comparator
            .comparing(ExpectedBenefit::annualExpectedBenefit)
            .reversed()
            .thenComparing(ExpectedBenefit::productId);

    private final Clock clock;

    public RankRecommendationsTool(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Class<RankRecommendationsInput> inputType() {
        return RankRecommendationsInput.class;
    }

    @Override
    public Class<RecommendationRankingResult> outputType() {
        return RecommendationRankingResult.class;
    }

    @Override
    public ToolExecutionResult<RecommendationRankingResult> execute(
            RankRecommendationsInput input,
            ToolExecutionContext context
    ) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Map<String, List<ExpectedBenefit>> benefitsByCustomer = groupByCustomer(
                input.benefitCalculationResult().benefits()
        );
        if (benefitsByCustomer.size() > context.maxResultCount()) {
            return ToolExecutionResult.rejected(NAME, "RESULT_LIMIT_EXCEEDED", clock.instant());
        }

        List<ProductRecommendation> recommendations = new ArrayList<>();
        List<NoRecommendation> noRecommendations = new ArrayList<>();
        benefitsByCustomer.forEach((customerKey, benefits) -> selectBest(benefits)
                .ifPresentOrElse(
                        benefit -> recommendations.add(toRecommendation(
                                benefit,
                                input.benefitCalculationResult().currency()
                        )),
                        () -> noRecommendations.add(new NoRecommendation(
                                customerKey,
                                List.of("NO_ELIGIBLE_PRODUCT")
                        ))
                ));

        RecommendationRankingResult result = new RecommendationRankingResult(
                RANKING_METRIC,
                recommendations,
                noRecommendations
        );
        return ToolExecutionResult.success(NAME, result, clock.instant());
    }

    private static Map<String, List<ExpectedBenefit>> groupByCustomer(List<ExpectedBenefit> benefits) {
        Map<String, List<ExpectedBenefit>> grouped = new LinkedHashMap<>();
        benefits.stream()
                .sorted(Comparator
                        .comparing(ExpectedBenefit::customerKey)
                        .thenComparing(ExpectedBenefit::productId))
                .forEach(benefit -> grouped
                        .computeIfAbsent(benefit.customerKey(), ignored -> new ArrayList<>())
                        .add(benefit));
        return grouped;
    }

    private static java.util.Optional<ExpectedBenefit> selectBest(List<ExpectedBenefit> benefits) {
        return benefits.stream()
                .filter(ExpectedBenefit::eligible)
                .sorted(BENEFIT_ORDER)
                .findFirst();
    }

    private static ProductRecommendation toRecommendation(ExpectedBenefit benefit, String currency) {
        List<String> reasonCodes = new ArrayList<>(benefit.reasonCodes());
        reasonCodes.add("HIGHEST_EXPECTED_ANNUAL_BENEFIT");
        return new ProductRecommendation(
                benefit.customerKey(),
                benefit.productId(),
                benefit.monthlyExpectedBenefit(),
                benefit.annualExpectedBenefit(),
                currency,
                reasonCodes
        );
    }
}
