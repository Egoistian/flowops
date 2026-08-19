package com.egoistian.flowops.procurement.infrastructure;

import com.egoistian.flowops.procurement.domain.ProcurementRequest;
import com.egoistian.flowops.procurement.domain.ProcurementStatus;
import com.egoistian.flowops.workflow.domain.ApprovalPlan;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "procurement_requests")
public class JpaProcurementRequestEntity {
    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String purpose;

    @Column(name = "budget_code", nullable = false)
    private String budgetCode;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcurementStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JpaProcurementItemEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JpaApprovalStepEntity> approvalSteps = new ArrayList<>();

    protected JpaProcurementRequestEntity() {
    }

    static JpaProcurementRequestEntity from(ProcurementRequest request) {
        JpaProcurementRequestEntity entity = new JpaProcurementRequestEntity();
        entity.id = request.id();
        entity.organizationId = request.organizationId();
        entity.requesterId = request.requesterId();
        entity.title = request.title();
        entity.purpose = request.purpose();
        entity.budgetCode = request.budgetCode();
        entity.totalAmount = BigDecimal.valueOf(request.totalAmount().amount());
        entity.currency = request.totalAmount().currency();
        entity.status = request.status();
        entity.version = request.persisted() ? request.version() : null;
        entity.createdAt = Instant.now();
        entity.updatedAt = entity.createdAt;
        entity.items = request.items().stream()
                .map(item -> JpaProcurementItemEntity.from(request.organizationId(), entity, item))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        entity.approvalSteps = request.approvalPlan()
                .map(ApprovalPlan::snapshots)
                .orElseGet(List::of)
                .stream()
                .map(step -> JpaApprovalStepEntity.from(request.organizationId(), entity, step))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        return entity;
    }

    ProcurementRequest toDomain() {
        return ProcurementRequest.restore(
                id,
                organizationId,
                requesterId,
                title,
                purpose,
                budgetCode,
                status,
                version == null ? 0 : version,
                items.stream().map(JpaProcurementItemEntity::toDomain).toList(),
                approvalSteps.isEmpty()
                        ? null
                        : ApprovalPlan.restore(approvalSteps.stream()
                                .map(JpaApprovalStepEntity::toSnapshot)
                                .toList()));
    }

    void apply(ProcurementRequest request) {
        if (!id.equals(request.id()) || !organizationId.equals(request.organizationId())) {
            throw new IllegalArgumentException("request identity or organization changed");
        }
        title = request.title();
        purpose = request.purpose();
        budgetCode = request.budgetCode();
        totalAmount = BigDecimal.valueOf(request.totalAmount().amount());
        currency = request.totalAmount().currency();
        status = request.status();
        updatedAt = Instant.now();
        if (submittedAt == null && request.status() != ProcurementStatus.DRAFT) {
            submittedAt = updatedAt;
        }

        request.approvalPlan().ifPresent(plan -> plan.snapshots().forEach(snapshot -> {
            approvalSteps.stream()
                    .filter(existing -> existing.sequenceNumber() == snapshot.sequence())
                    .findFirst()
                    .ifPresentOrElse(
                            existing -> existing.apply(snapshot),
                            () -> approvalSteps.add(JpaApprovalStepEntity.from(
                                    organizationId, this, snapshot)));
        }));
    }
}
