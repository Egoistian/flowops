package com.egoistian.flowops.procurement.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcurementApprovalPolicyTest {
    private final ProcurementApprovalPolicy policy = new ProcurementApprovalPolicy();

    @Test
    void selectsExactApprovalRolesAtAmountBoundaries() {
        assertThat(policy.createPlan(Money.krw(999_999)).requiredRoles())
                .containsExactly("REVIEWER");
        assertThat(policy.createPlan(Money.krw(1_000_000)).requiredRoles())
                .containsExactly("REVIEWER", "MANAGER");
        assertThat(policy.createPlan(Money.krw(5_000_000)).requiredRoles())
                .containsExactly("REVIEWER", "MANAGER", "BUDGET_OWNER");
    }
}
