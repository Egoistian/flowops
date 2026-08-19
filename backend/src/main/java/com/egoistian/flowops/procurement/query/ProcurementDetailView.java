package com.egoistian.flowops.procurement.query;

import com.egoistian.flowops.procurement.domain.ProcurementStatus;

import java.util.List;
import java.util.UUID;

public record ProcurementDetailView(
        UUID id,
        String title,
        String purpose,
        String budgetCode,
        long totalAmountKrw,
        ProcurementStatus status,
        long version,
        List<ItemView> items) {
    public record ItemView(
            String name,
            int quantity,
            long unitPriceKrw,
            long subtotalKrw) {
    }
}
