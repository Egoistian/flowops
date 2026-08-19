package com.egoistian.flowops.procurement.domain;

public record Money(long amount, String currency) {
    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        if (!"KRW".equals(currency)) {
            throw new IllegalArgumentException("currency must be KRW");
        }
    }

    public static Money krw(long amount) {
        return new Money(amount, "KRW");
    }

    public Money multiply(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return krw(Math.multiplyExact(amount, quantity));
    }

    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("currency mismatch");
        }
        return krw(Math.addExact(amount, other.amount));
    }
}
