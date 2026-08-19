package com.egoistian.flowops.procurement.domain;

import com.egoistian.flowops.workflow.domain.ApprovalPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ProcurementRequest {
    private final UUID id;
    private final UUID organizationId;
    private final UUID requesterId;
    private final String title;
    private final String purpose;
    private final String budgetCode;
    private final List<ProcurementItem> items = new ArrayList<>();
    private ProcurementStatus status;
    private ApprovalPlan approvalPlan;

    private ProcurementRequest(
            UUID id,
            UUID organizationId,
            UUID requesterId,
            String title,
            String purpose,
            String budgetCode) {
        this.id = requireId(id, "id");
        this.organizationId = requireId(organizationId, "organizationId");
        this.requesterId = requireId(requesterId, "requesterId");
        this.title = requireText(title, "title");
        this.purpose = requireText(purpose, "purpose");
        this.budgetCode = requireText(budgetCode, "budgetCode");
        this.status = ProcurementStatus.DRAFT;
    }

    public static ProcurementRequest draft(
            UUID organizationId,
            UUID requesterId,
            String title,
            String purpose,
            String budgetCode) {
        return new ProcurementRequest(
                UUID.randomUUID(), organizationId, requesterId,
                title, purpose, budgetCode);
    }

    public void addItem(String name, int quantity, Money unitPrice) {
        requireStatus(ProcurementStatus.DRAFT);
        items.add(new ProcurementItem(name, quantity, unitPrice));
    }

    public Money totalAmount() {
        return items.stream()
                .map(ProcurementItem::subtotal)
                .reduce(Money.krw(0), Money::add);
    }

    public void submit(UUID actorId, ProcurementApprovalPolicy policy) {
        if (!requesterId.equals(actorId)) {
            throw new DomainRuleViolation("REQUESTER_REQUIRED");
        }
        requireStatus(ProcurementStatus.DRAFT);
        if (items.isEmpty()) {
            throw new DomainRuleViolation("PROCUREMENT_ITEMS_REQUIRED");
        }
        approvalPlan = policy.createPlan(totalAmount());
        status = ProcurementStatus.SUBMITTED;
    }

    public void approve(UUID actorId, Set<String> actorRoles) {
        if (requesterId.equals(actorId)) {
            throw new DomainRuleViolation("SELF_APPROVAL_FORBIDDEN");
        }
        if (status != ProcurementStatus.SUBMITTED && status != ProcurementStatus.IN_REVIEW) {
            throw new DomainRuleViolation("INVALID_PROCUREMENT_TRANSITION");
        }
        String requiredRole = approvalPlan.currentRequiredRole()
                .orElseThrow(() -> new DomainRuleViolation("INVALID_PROCUREMENT_TRANSITION"));
        if (!actorRoles.contains(requiredRole)) {
            throw new DomainRuleViolation("APPROVAL_ROLE_REQUIRED");
        }
        approvalPlan.approveCurrent(actorId);
        status = approvalPlan.isComplete()
                ? ProcurementStatus.APPROVED
                : ProcurementStatus.IN_REVIEW;
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID requesterId() {
        return requesterId;
    }

    public String title() {
        return title;
    }

    public String purpose() {
        return purpose;
    }

    public String budgetCode() {
        return budgetCode;
    }

    public List<ProcurementItem> items() {
        return List.copyOf(items);
    }

    public ProcurementStatus status() {
        return status;
    }

    private void requireStatus(ProcurementStatus expected) {
        if (status != expected) {
            throw new DomainRuleViolation("INVALID_PROCUREMENT_TRANSITION");
        }
    }

    private static UUID requireId(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
