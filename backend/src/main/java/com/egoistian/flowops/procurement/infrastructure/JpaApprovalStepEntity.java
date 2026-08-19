package com.egoistian.flowops.procurement.infrastructure;

import com.egoistian.flowops.workflow.domain.ApprovalStepSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_steps")
public class JpaApprovalStepEntity {
    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private JpaProcurementRequestEntity request;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "required_role", nullable = false)
    private String requiredRole;

    @Column(name = "assigned_reviewer_id")
    private UUID assignedReviewerId;

    @Column(nullable = false)
    private String status;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JpaApprovalStepEntity() {
    }

    static JpaApprovalStepEntity from(
            UUID organizationId,
            JpaProcurementRequestEntity request,
            ApprovalStepSnapshot snapshot) {
        JpaApprovalStepEntity entity = new JpaApprovalStepEntity();
        entity.id = UUID.randomUUID();
        entity.organizationId = organizationId;
        entity.request = request;
        entity.sequenceNumber = snapshot.sequence();
        entity.requiredRole = snapshot.requiredRole();
        entity.assignedReviewerId = snapshot.approvedBy();
        entity.status = snapshot.approvedBy() == null ? "PENDING" : "APPROVED";
        entity.decidedAt = snapshot.approvedBy() == null ? null : Instant.now();
        entity.createdAt = Instant.now();
        return entity;
    }

    ApprovalStepSnapshot toSnapshot() {
        return new ApprovalStepSnapshot(sequenceNumber, requiredRole,
                "APPROVED".equals(status) ? assignedReviewerId : null);
    }

    int sequenceNumber() {
        return sequenceNumber;
    }

    void apply(ApprovalStepSnapshot snapshot) {
        requiredRole = snapshot.requiredRole();
        assignedReviewerId = snapshot.approvedBy();
        status = snapshot.approvedBy() == null ? "PENDING" : "APPROVED";
        decidedAt = snapshot.approvedBy() == null ? null : Instant.now();
    }
}
