package com.egoistian.flowops.procurement.domain;

import com.egoistian.flowops.workflow.domain.ApprovalPlan;
import com.egoistian.flowops.workflow.domain.ApprovalStepDefinition;

import java.util.List;

public class ProcurementApprovalPolicy {
    public ApprovalPlan createPlan(Money totalAmount) {
        if (totalAmount.amount() < 1_000_000) {
            return plan("REVIEWER");
        }
        if (totalAmount.amount() < 5_000_000) {
            return plan("REVIEWER", "MANAGER");
        }
        return plan("REVIEWER", "MANAGER", "BUDGET_OWNER");
    }

    private ApprovalPlan plan(String... roles) {
        List<ApprovalStepDefinition> steps = java.util.stream.IntStream.range(0, roles.length)
                .mapToObj(index -> new ApprovalStepDefinition(index + 1, roles[index]))
                .toList();
        return new ApprovalPlan(steps);
    }
}
