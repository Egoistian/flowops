package com.egoistian.flowops.procurement;

import com.egoistian.flowops.procurement.application.ProcurementCommands.SubmitCommand;
import com.egoistian.flowops.procurement.application.ProcurementCommands.SubmitResult;
import com.egoistian.flowops.procurement.application.SubmitProcurementRequest;
import com.egoistian.flowops.procurement.domain.Money;
import com.egoistian.flowops.procurement.domain.ProcurementRequest;
import com.egoistian.flowops.procurement.domain.ProcurementStatus;
import com.egoistian.flowops.procurement.infrastructure.ProcurementStore;
import com.egoistian.flowops.testsupport.PostgresIntegrationTest;
import com.egoistian.flowops.shared.idempotency.IdempotencyKeyReused;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;

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
class IdempotentSubmissionTest extends PostgresIntegrationTest {
    private static final UUID ORGANIZATION_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID REQUESTER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111101");

    @Autowired
    ProcurementStore store;

    @Autowired
    SubmitProcurementRequest submitProcurementRequest;

    @Autowired
    JdbcClient jdbc;

    @Test
    void replaysTheFirstResultWithoutSubmittingOrAuditingTwice() {
        ProcurementRequest request = savedDraft();
        SubmitCommand command = new SubmitCommand(
                ORGANIZATION_ID,
                request.id(),
                REQUESTER_ID,
                "submit-001",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        SubmitResult first = submitProcurementRequest.handle(command);
        SubmitResult replay = submitProcurementRequest.handle(command);

        assertThat(replay).isEqualTo(first);
        assertThat(replay.requestId()).isEqualTo(request.id());
        assertThat(replay.status()).isEqualTo(ProcurementStatus.SUBMITTED);
        assertThat(count("audit_events", "event_type = 'REQUEST_SUBMITTED'")).isEqualTo(1);
        assertThat(count("idempotency_records", "operation = 'PROCUREMENT_SUBMIT'")).isEqualTo(1);
    }

    @Test
    void rejectsTheSameIdempotencyKeyWithDifferentContent() {
        ProcurementRequest request = savedDraft();
        submitProcurementRequest.handle(new SubmitCommand(
                ORGANIZATION_ID,
                request.id(),
                REQUESTER_ID,
                "submit-001",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));

        assertThatThrownBy(() -> submitProcurementRequest.handle(new SubmitCommand(
                ORGANIZATION_ID,
                request.id(),
                REQUESTER_ID,
                "submit-001",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")))
                .isInstanceOf(IdempotencyKeyReused.class)
                .extracting("code")
                .isEqualTo("IDEMPOTENCY_KEY_REUSED");
    }

    private ProcurementRequest savedDraft() {
        ProcurementRequest request = ProcurementRequest.draft(
                ORGANIZATION_ID, REQUESTER_ID, "개발용 장비", "통합 테스트 환경", "ENG-2026");
        request.addItem("노트북", 1, Money.krw(900_000));
        return store.save(request);
    }

    private int count(String table, String condition) {
        return jdbc.sql("select count(*) from " + table + " where " + condition)
                .query(Integer.class)
                .single();
    }
}
