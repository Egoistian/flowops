package com.egoistian.flowops.workflow.domain;

import java.util.UUID;

public record ApprovalStepSnapshot(int sequence, String requiredRole, UUID approvedBy) {
}
