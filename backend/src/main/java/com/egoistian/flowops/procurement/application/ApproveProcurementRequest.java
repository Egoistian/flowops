package com.egoistian.flowops.procurement.application;

import com.egoistian.flowops.audit.AuditEventWriter;
import com.egoistian.flowops.procurement.application.ProcurementCommands.ApproveCommand;
import com.egoistian.flowops.procurement.domain.ProcurementRequest;
import com.egoistian.flowops.procurement.infrastructure.ProcurementStore;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApproveProcurementRequest {
    private final ProcurementStore store;
    private final AuditEventWriter audit;

    public ApproveProcurementRequest(
            ProcurementStore store,
            AuditEventWriter audit) {
        this.store = store;
        this.audit = audit;
    }

    @Transactional
    public ApprovalResult handle(ApproveCommand command) {
        ProcurementRequest request = store.find(
                        command.organizationId(), command.requestId())
                .orElseThrow(ProcurementNotFound::new);
        if (request.version() != command.expectedVersion()) {
            throw new RequestVersionConflict("REQUEST_VERSION_CONFLICT");
        }
        request.approve(command.actorId(), command.actorRoles());
        try {
            ProcurementRequest saved = store.save(request);
            audit.append(
                    saved.organizationId(),
                    "PROCUREMENT",
                    saved.id(),
                    "REQUEST_APPROVED",
                    command.actorId());
            return new ApprovalResult(
                    saved.id(), saved.status(), saved.version());
        } catch (OptimisticLockingFailureException conflict) {
            throw new RequestVersionConflict(
                    "REQUEST_VERSION_CONFLICT", conflict);
        }
    }
}
