package com.egoistian.flowops.procurement;

import com.egoistian.flowops.procurement.domain.Money;
import com.egoistian.flowops.procurement.domain.ProcurementRequest;
import com.egoistian.flowops.procurement.domain.ProcurementStatus;
import com.egoistian.flowops.procurement.infrastructure.ProcurementStore;
import com.egoistian.flowops.testsupport.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Sql(scripts = "/db/testdata/demo_accounts.sql")
@Sql(statements = {
        "delete from approval_steps",
        "delete from procurement_items",
        "delete from procurement_requests",
        "delete from user_roles",
        "delete from users",
        "delete from organizations"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ProcurementPersistenceTest extends PostgresIntegrationTest {
    private static final UUID ORGANIZATION_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID REQUESTER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111101");

    @Autowired
    ProcurementStore store;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndReloadsARequestInsideItsOrganizationScope() {
        ProcurementRequest request = ProcurementRequest.draft(
                ORGANIZATION_ID, REQUESTER_ID, "개발용 장비", "통합 테스트 환경", "ENG-2026");
        request.addItem("노트북", 2, Money.krw(1_200_000));
        request.addItem("도킹 스테이션", 1, Money.krw(180_000));

        ProcurementRequest saved = store.save(request);
        entityManager.flush();
        entityManager.clear();

        ProcurementRequest reloaded = store.find(ORGANIZATION_ID, saved.id()).orElseThrow();

        assertThat(reloaded.title()).isEqualTo("개발용 장비");
        assertThat(reloaded.items()).extracting("name")
                .containsExactlyInAnyOrder("노트북", "도킹 스테이션");
        assertThat(reloaded.totalAmount()).isEqualTo(Money.krw(2_580_000));
        assertThat(reloaded.status()).isEqualTo(ProcurementStatus.DRAFT);
        assertThat(reloaded.version()).isZero();
    }
}
