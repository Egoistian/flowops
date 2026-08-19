package com.egoistian.flowops.procurement;

import com.egoistian.flowops.procurement.application.ApprovalResult;
import com.egoistian.flowops.procurement.application.ApproveProcurementRequest;
import com.egoistian.flowops.procurement.application.ProcurementCommands.ApproveCommand;
import com.egoistian.flowops.procurement.application.ProcurementCommands.SubmitCommand;
import com.egoistian.flowops.procurement.application.ProcurementCommands.SubmitResult;
import com.egoistian.flowops.procurement.application.RequestVersionConflict;
import com.egoistian.flowops.procurement.application.SubmitProcurementRequest;
import com.egoistian.flowops.procurement.domain.Money;
import com.egoistian.flowops.procurement.domain.ProcurementRequest;
import com.egoistian.flowops.procurement.domain.ProcurementStatus;
import com.egoistian.flowops.procurement.infrastructure.ProcurementStore;
import com.egoistian.flowops.testsupport.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Sql(scripts = "/db/testdata/demo_accounts.sql")
@Sql(statements = {
        "delete from audit_events",
        "delete from idempotency_records",
        "delete from approval_steps",
        "delete from procurement_items",
        "delete from procurement_requests",
        "delete from user_roles",
        "delete from users",
        "delete from organizations"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ConcurrentApprovalTest extends PostgresIntegrationTest {
    private static final UUID ORGANIZATION_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID REQUESTER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111101");
    private static final UUID REVIEWER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111102");

    @Autowired
    ProcurementStore store;

    @Autowired
    SubmitProcurementRequest submitProcurementRequest;

    @Autowired
    ApproveProcurementRequest approveProcurementRequest;

    @Autowired
    JdbcClient jdbc;

    @Test
    void rejectsAnApprovalBasedOnAStaleRequestVersion() {
        ProcurementRequest draft = ProcurementRequest.draft(
                ORGANIZATION_ID, REQUESTER_ID, "개발용 장비", "통합 테스트 환경", "ENG-2026");
        draft.addItem("노트북", 1, Money.krw(900_000));
        ProcurementRequest saved = store.save(draft);
        SubmitResult submitted = submitProcurementRequest.handle(new SubmitCommand(
                ORGANIZATION_ID,
                saved.id(),
                REQUESTER_ID,
                "submit-concurrent-001",
                "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"));

        ApproveCommand staleCommand = new ApproveCommand(
                ORGANIZATION_ID,
                saved.id(),
                REVIEWER_ID,
                Set.of("REVIEWER"),
                submitted.version());

        ApprovalResult first = approveProcurementRequest.handle(staleCommand);
        assertThat(first.status()).isEqualTo(ProcurementStatus.APPROVED);

        assertThatThrownBy(() -> approveProcurementRequest.handle(staleCommand))
                .isInstanceOf(RequestVersionConflict.class)
                .extracting("code")
                .isEqualTo("REQUEST_VERSION_CONFLICT");

        Integer approvalAuditCount = jdbc.sql("""
                        select count(*) from audit_events
                        where event_type = 'REQUEST_APPROVED'
                        """).query(Integer.class).single();
        assertThat(approvalAuditCount).isEqualTo(1);
    }
}
