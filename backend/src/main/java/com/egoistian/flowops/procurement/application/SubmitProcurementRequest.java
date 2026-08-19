package com.egoistian.flowops.procurement.application;

import com.egoistian.flowops.audit.AuditEventWriter;
import com.egoistian.flowops.procurement.application.ProcurementCommands.SubmitCommand;
import com.egoistian.flowops.procurement.application.ProcurementCommands.SubmitResult;
import com.egoistian.flowops.procurement.domain.ProcurementApprovalPolicy;
import com.egoistian.flowops.procurement.domain.ProcurementRequest;
import com.egoistian.flowops.procurement.infrastructure.ProcurementStore;
import com.egoistian.flowops.shared.idempotency.IdempotencyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmitProcurementRequest {
    private final ProcurementStore store;
    private final ProcurementApprovalPolicy approvalPolicy;
    private final IdempotencyService idempotency;
    private final AuditEventWriter audit;

    public SubmitProcurementRequest(
            ProcurementStore store,
            ProcurementApprovalPolicy approvalPolicy,
            IdempotencyService idempotency,
            AuditEventWriter audit) {
        this.store = store;
        this.approvalPolicy = approvalPolicy;
        this.idempotency = idempotency;
        this.audit = audit;
    }

    @Transactional
    public SubmitResult handle(SubmitCommand command) {
        return idempotency.execute(
                command.organizationId(),
                "PROCUREMENT_SUBMIT",
                command.idempotencyKey(),
                command.requestHash(),
                SubmitResult.class,
                () -> {
                    ProcurementRequest request = store.find(
                                    command.organizationId(), command.requestId())
                            .orElseThrow(ProcurementNotFound::new);
                    request.submit(command.actorId(), approvalPolicy);
                    ProcurementRequest saved = store.save(request);
                    audit.append(
                            saved.organizationId(),
                            "PROCUREMENT",
                            saved.id(),
                            "REQUEST_SUBMITTED",
                            command.actorId());
                    return new SubmitResult(
                            saved.id(), saved.status(), saved.version());
                });
    }
}
