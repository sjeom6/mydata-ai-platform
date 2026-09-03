package com.sjeom.mydata.platform.tool.consumption;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CardTransaction(
        String transactionId,
        String customerKey,
        LocalDate occurredOn,
        ConsumptionCategory category,
        BigDecimal amount,
        TransactionStatus status,
        String originalTransactionId
) {

    public CardTransaction {
        transactionId = requireText(transactionId, "transactionId");
        customerKey = requireText(customerKey, "customerKey");
        if (occurredOn == null) {
            throw new IllegalArgumentException("occurredOn must not be null");
        }
        if (category == null) {
            throw new IllegalArgumentException("category must not be null");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (status != TransactionStatus.APPROVED) {
            originalTransactionId = requireText(originalTransactionId, "originalTransactionId");
        }
    }

    public BigDecimal signedAmount() {
        return status == TransactionStatus.APPROVED ? amount : amount.negate();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
