package com.egoistian.flowops.workflow.domain;

public record ApprovalStepDefinition(int sequence, String requiredRole) {
    public ApprovalStepDefinition {
        if (sequence <= 0) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        if (requiredRole == null || requiredRole.isBlank()) {
            throw new IllegalArgumentException("requiredRole must not be blank");
        }
    }
}
