package com.egoistian.flowops.workflow.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ApprovalPlan {
    private final List<ApprovalStep> steps;

    public ApprovalPlan(List<ApprovalStepDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("at least one approval step is required");
        }
        this.steps = definitions.stream()
                .map(definition -> new ApprovalStep(
                        definition.sequence(), definition.requiredRole()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private ApprovalPlan(List<ApprovalStepSnapshot> snapshots, boolean restored) {
        this.steps = snapshots.stream()
                .map(snapshot -> new ApprovalStep(
                        snapshot.sequence(), snapshot.requiredRole(), snapshot.approvedBy()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public static ApprovalPlan restore(List<ApprovalStepSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            throw new IllegalArgumentException("at least one approval step is required");
        }
        return new ApprovalPlan(snapshots, true);
    }

    public List<String> requiredRoles() {
        return steps.stream().map(ApprovalStep::requiredRole).toList();
    }

    public Optional<String> currentRequiredRole() {
        return steps.stream()
                .filter(step -> step.approvedBy() == null)
                .map(ApprovalStep::requiredRole)
                .findFirst();
    }

    public void approveCurrent(UUID actorId) {
        ApprovalStep current = steps.stream()
                .filter(step -> step.approvedBy() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("approval plan is complete"));
        current.approve(actorId);
    }

    public boolean isComplete() {
        return steps.stream().allMatch(step -> step.approvedBy() != null);
    }

    public List<ApprovalStepSnapshot> snapshots() {
        return steps.stream()
                .map(step -> new ApprovalStepSnapshot(
                        step.sequence(), step.requiredRole(), step.approvedBy()))
                .toList();
    }

    private static final class ApprovalStep {
        private final int sequence;
        private final String requiredRole;
        private UUID approvedBy;

        private ApprovalStep(int sequence, String requiredRole) {
            this(sequence, requiredRole, null);
        }

        private ApprovalStep(int sequence, String requiredRole, UUID approvedBy) {
            this.sequence = sequence;
            this.requiredRole = requiredRole;
            this.approvedBy = approvedBy;
        }

        int sequence() {
            return sequence;
        }

        String requiredRole() {
            return requiredRole;
        }

        UUID approvedBy() {
            return approvedBy;
        }

        void approve(UUID actorId) {
            approvedBy = actorId;
        }
    }
}
