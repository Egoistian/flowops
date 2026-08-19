package com.egoistian.flowops.procurement.application;

import com.egoistian.flowops.procurement.domain.ProcurementStatus;

import java.util.Set;
import java.util.List;
import java.util.UUID;

public final class ProcurementCommands {
    private ProcurementCommands() {
    }

    public record CreateDraftCommand(
            UUID organizationId,
            UUID requesterId,
            String title,
            String purpose,
            String budgetCode,
            List<CreateDraftItem> items) {
        public CreateDraftCommand {
            items = List.copyOf(items);
        }
    }

    public record CreateDraftItem(String name, int quantity, long unitPriceKrw) {
    }

    public record DraftResult(
            UUID requestId,
            ProcurementStatus status,
            long totalAmountKrw,
            long version) {
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
