package com.egoistian.flowops.procurement.query;

import com.egoistian.flowops.procurement.application.ProcurementNotFound;
import com.egoistian.flowops.procurement.domain.ProcurementRequest;
import com.egoistian.flowops.procurement.infrastructure.ProcurementStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProcurementQueryService {
    private final ProcurementStore store;

    public ProcurementQueryService(ProcurementStore store) {
        this.store = store;
    }

    @Transactional(readOnly = true)
    public ProcurementDetailView get(UUID organizationId, UUID requestId) {
        ProcurementRequest request = store.find(organizationId, requestId)
                .orElseThrow(ProcurementNotFound::new);
        return new ProcurementDetailView(
                request.id(),
                request.title(),
                request.purpose(),
                request.budgetCode(),
                request.totalAmount().amount(),
                request.status(),
                request.version(),
                request.items().stream()
                        .map(item -> new ProcurementDetailView.ItemView(
                                item.name(),
                                item.quantity(),
                                item.unitPrice().amount(),
                                item.subtotal().amount()))
                        .toList());
    }
}
