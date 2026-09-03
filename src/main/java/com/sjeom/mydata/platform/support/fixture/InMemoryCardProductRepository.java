package com.sjeom.mydata.platform.support.fixture;

import com.sjeom.mydata.platform.product.domain.CardProduct;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import com.sjeom.mydata.platform.tool.product.CardProductRepository;
import java.util.Comparator;
import java.util.List;

public final class InMemoryCardProductRepository implements CardProductRepository {

    private final List<CardProduct> products;

    public InMemoryCardProductRepository(List<CardProduct> products) {
        this.products = List.copyOf(products);
    }

    @Override
    public List<CardProduct> findByBenefitCategory(ConsumptionCategory benefitCategory) {
        return products.stream()
                .filter(product -> product.benefitCategory() == benefitCategory)
                .sorted(Comparator.comparing(CardProduct::productId))
                .toList();
    }
}
