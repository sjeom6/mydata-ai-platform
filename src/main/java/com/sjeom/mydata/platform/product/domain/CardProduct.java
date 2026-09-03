package com.sjeom.mydata.platform.product.domain;

import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CardProduct(
        String productId,
        String name,
        ProductSaleStatus saleStatus,
        LocalDate validFrom,
        LocalDate validTo,
        ConsumptionCategory benefitCategory,
        BigDecimal discountRate,
        BigDecimal monthlyDiscountLimit,
        BigDecimal minimumPreviousMonthSpend,
        boolean complianceApproved
) {

    public CardProduct {
        productId = requireText(productId, "productId");
        name = requireText(name, "name");
        if (saleStatus == null) {
            throw new IllegalArgumentException("saleStatus must not be null");
        }
        if (validFrom == null || validTo == null || validFrom.isAfter(validTo)) {
            throw new IllegalArgumentException("product validity period is invalid");
        }
        if (benefitCategory == null) {
            throw new IllegalArgumentException("benefitCategory must not be null");
        }
        validateRate(discountRate);
        validateNonNegative(monthlyDiscountLimit, "monthlyDiscountLimit");
        validateNonNegative(minimumPreviousMonthSpend, "minimumPreviousMonthSpend");
    }

    public boolean isValidOn(LocalDate date) {
        return !date.isBefore(validFrom) && !date.isAfter(validTo);
    }

    private static void validateRate(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("discountRate must be between zero and one");
        }
    }

    private static void validateNonNegative(BigDecimal value, String fieldName) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
