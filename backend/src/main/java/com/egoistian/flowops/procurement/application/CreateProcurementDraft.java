package com.egoistian.flowops.procurement.application;

import com.egoistian.flowops.audit.AuditEventWriter;
import com.egoistian.flowops.procurement.application.ProcurementCommands.CreateDraftCommand;
import com.egoistian.flowops.procurement.application.ProcurementCommands.DraftResult;
import com.egoistian.flowops.procurement.domain.Money;
import com.egoistian.flowops.procurement.domain.ProcurementRequest;
import com.egoistian.flowops.procurement.infrastructure.ProcurementStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProcurementDraft {
    private final ProcurementStore store;
    private final AuditEventWriter audit;

    public CreateProcurementDraft(
            ProcurementStore store,
            AuditEventWriter audit) {
        this.store = store;
        this.audit = audit;
    }

    @Transactional
    public DraftResult handle(CreateDraftCommand command) {
        ProcurementRequest request = ProcurementRequest.draft(
                command.organizationId(),
                command.requesterId(),
                command.title(),
                command.purpose(),
                command.budgetCode());
        command.items().forEach(item -> request.addItem(
                item.name(), item.quantity(), Money.krw(item.unitPriceKrw())));
        ProcurementRequest saved = store.save(request);
        audit.append(
                saved.organizationId(),
                "PROCUREMENT",
                saved.id(),
                "DRAFT_CREATED",
                command.requesterId());
        return new DraftResult(
                saved.id(),
                saved.status(),
                saved.totalAmount().amount(),
                saved.version());
    }
}
