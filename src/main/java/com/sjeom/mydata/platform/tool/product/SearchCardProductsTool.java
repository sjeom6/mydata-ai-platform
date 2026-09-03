package com.sjeom.mydata.platform.tool.product;

import com.sjeom.mydata.platform.product.domain.CardProduct;
import com.sjeom.mydata.platform.product.domain.ProductCandidate;
import com.sjeom.mydata.platform.product.domain.ProductSaleStatus;
import com.sjeom.mydata.platform.tool.domain.AiDataTool;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionResult;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class SearchCardProductsTool
        implements AiDataTool<SearchCardProductsInput, CardProductSearchResult> {

    public static final String NAME = "SEARCH_CARD_PRODUCTS";

    private final CardProductRepository repository;
    private final Clock clock;

    public SearchCardProductsTool(CardProductRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Class<SearchCardProductsInput> inputType() {
        return SearchCardProductsInput.class;
    }

    @Override
    public Class<CardProductSearchResult> outputType() {
        return CardProductSearchResult.class;
    }

    @Override
    public ToolExecutionResult<CardProductSearchResult> execute(
            SearchCardProductsInput input,
            ToolExecutionContext context
    ) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(context, "context must not be null");

        List<CardProduct> products = Objects.requireNonNull(
                repository.findByBenefitCategory(input.benefitCategory()),
                "repository result must not be null"
        );

        List<ProductCandidate> candidates = products.stream()
                .filter(product -> product.benefitCategory() == input.benefitCategory())
                .filter(product -> product.saleStatus() == ProductSaleStatus.ON_SALE)
                .filter(product -> product.isValidOn(context.dataAsOf()))
                .filter(CardProduct::complianceApproved)
                .map(ProductCandidate::from)
                .sorted(Comparator.comparing(ProductCandidate::productId))
                .toList();

        if (candidates.size() > context.maxResultCount()) {
            return ToolExecutionResult.rejected(NAME, "RESULT_LIMIT_EXCEEDED", clock.instant());
        }

        CardProductSearchResult result = new CardProductSearchResult(
                input.benefitCategory(),
                context.dataAsOf(),
                candidates
        );
        return ToolExecutionResult.success(NAME, result, clock.instant());
    }
}
