package com.egoistian.flowops.procurement.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcurementRequestTest {
    private static final UUID ORGANIZATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID REQUESTER_ID = UUID.fromString("11111111-1111-1111-1111-111111111101");
    private static final UUID REVIEWER_ID = UUID.fromString("11111111-1111-1111-1111-111111111102");

    @Test
    void calculatesTheTotalFromItemsInsteadOfTrustingAClientTotal() {
        ProcurementRequest request = ProcurementRequest.draft(
                ORGANIZATION_ID, REQUESTER_ID, "개발용 장비", "테스트 환경 구성", "ENG-2026");
        request.addItem("노트북", 2, Money.krw(1_200_000));
        request.addItem("도킹 스테이션", 1, Money.krw(180_000));

        assertThat(request.totalAmount()).isEqualTo(Money.krw(2_580_000));
    }

    @Test
    void rejectsSubmissionWithoutAnItem() {
        ProcurementRequest request = ProcurementRequest.draft(
                ORGANIZATION_ID, REQUESTER_ID, "빈 요청", "품목 없음", "ENG-2026");

        assertThatThrownBy(() -> request.submit(REQUESTER_ID, new ProcurementApprovalPolicy()))
                .isInstanceOf(DomainRuleViolation.class)
                .extracting("code")
                .isEqualTo("PROCUREMENT_ITEMS_REQUIRED");
    }

    @Test
    void preventsTheRequesterFromApprovingTheirOwnRequest() {
        ProcurementRequest request = submittedRequest();

        assertThatThrownBy(() -> request.approve(REQUESTER_ID, Set.of("REVIEWER")))
                .isInstanceOf(DomainRuleViolation.class)
                .extracting("code")
                .isEqualTo("SELF_APPROVAL_FORBIDDEN");
    }

    @Test
    void approvesOneStepWithTheRequiredRole() {
        ProcurementRequest request = submittedRequest();

        request.approve(REVIEWER_ID, Set.of("REVIEWER"));

        assertThat(request.status()).isEqualTo(ProcurementStatus.APPROVED);
    }

    @Test
    void rejectsARoleThatDoesNotMatchTheCurrentStep() {
        ProcurementRequest request = submittedRequest();

        assertThatThrownBy(() -> request.approve(REVIEWER_ID, Set.of("MANAGER")))
                .isInstanceOf(DomainRuleViolation.class)
                .extracting("code")
                .isEqualTo("APPROVAL_ROLE_REQUIRED");
    }

    @Test
    void advancesAMultiStepPlanOneRoleAtATime() {
        ProcurementRequest request = ProcurementRequest.draft(
                ORGANIZATION_ID, REQUESTER_ID, "서버 장비", "통합 테스트 환경", "ENG-2026");
        request.addItem("서버", 1, Money.krw(1_500_000));
        request.submit(REQUESTER_ID, new ProcurementApprovalPolicy());

        request.approve(REVIEWER_ID, Set.of("REVIEWER"));
        assertThat(request.status()).isEqualTo(ProcurementStatus.IN_REVIEW);

        request.approve(UUID.fromString("11111111-1111-1111-1111-111111111103"), Set.of("MANAGER"));
        assertThat(request.status()).isEqualTo(ProcurementStatus.APPROVED);
    }

    private ProcurementRequest submittedRequest() {
        ProcurementRequest request = ProcurementRequest.draft(
                ORGANIZATION_ID, REQUESTER_ID, "개발용 장비", "테스트 환경 구성", "ENG-2026");
        request.addItem("노트북", 1, Money.krw(900_000));
        request.submit(REQUESTER_ID, new ProcurementApprovalPolicy());
        return request;
    }
}
