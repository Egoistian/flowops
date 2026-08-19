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

    private static final class ApprovalStep {
        private final int sequence;
        private final String requiredRole;
        private UUID approvedBy;

        private ApprovalStep(int sequence, String requiredRole) {
            this.sequence = sequence;
            this.requiredRole = requiredRole;
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
