package com.egoistian.flowops.procurement.application;

import com.egoistian.flowops.procurement.domain.ProcurementStatus;

import java.util.UUID;

public record ApprovalResult(UUID requestId, ProcurementStatus status, long version) {
}
