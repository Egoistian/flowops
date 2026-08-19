package com.egoistian.flowops.procurement.domain;

public record ProcurementItem(String name, int quantity, Money unitPrice) {
    public ProcurementItem {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("unitPrice is required");
        }
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
