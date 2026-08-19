package com.egoistian.flowops.procurement.application;

import com.egoistian.flowops.procurement.domain.ProcurementStatus;

import java.util.Set;
import java.util.UUID;

public final class ProcurementCommands {
    private ProcurementCommands() {
    }

    public record SubmitCommand(
            UUID organizationId,
            UUID requestId,
            UUID actorId,
            String idempotencyKey,
            String requestHash) {
    }

    public record SubmitResult(UUID requestId, ProcurementStatus status, long version) {
    }

    public record ApproveCommand(
            UUID organizationId,
            UUID requestId,
            UUID actorId,
            Set<String> actorRoles,
            long expectedVersion) {
        public ApproveCommand {
            actorRoles = Set.copyOf(actorRoles);
        }
    }
}
