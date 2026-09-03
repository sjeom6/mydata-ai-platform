package com.sjeom.mydata.platform.tool.product;

import com.sjeom.mydata.platform.product.domain.CardProduct;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import java.util.List;

public interface CardProductRepository {

    List<CardProduct> findByBenefitCategory(ConsumptionCategory benefitCategory);
}
