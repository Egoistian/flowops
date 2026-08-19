# INC-001 — 승인 단계 자식 행이 중복 생성되는 문제

분류: 개발 중 재현한 문제와 해결 기록
실제 고객·프로덕션 사고 여부: 해당 없음

## 문제 상황

구매 요청을 제출하면 승인 계획이 DB에 생성됩니다. 이후 검토자가 승인할 때 DB에서 aggregate를 복원하고, 변경된 상태를 다시 저장했습니다. 첫 구현은 복원된 aggregate 전체를 새로운 JPA entity graph로 재생성했습니다.

## 영향

기존 승인 단계의 DB ID가 도메인 snapshot에 없었기 때문에 저장 시 같은 순서의 새 자식 행이 만들어졌습니다. 고유 제약이 없다면 승인 단계가 중복될 수 있고, 현재 schema에서는 요청 저장이 실패합니다. 승인 결정과 감사 로그가 함께 완료되지 못하는 문제입니다.

## 재현

```bash
cd backend
./gradlew test --tests com.egoistian.flowops.procurement.ConcurrentApprovalTest
```

테스트는 요청을 임시 저장하고 제출해 승인 단계를 만든 뒤, version을 포함한 승인 명령을 실행합니다.

## 기대 결과

- 첫 승인은 한 번 성공합니다.
- 승인 단계 한 행이 `APPROVED`로 갱신됩니다.
- 요청 version이 증가합니다.
- 같은 오래된 version의 두 번째 승인은 `REQUEST_VERSION_CONFLICT`로 거부됩니다.
- `REQUEST_APPROVED` 감사 이벤트는 한 건입니다.

## 수정 전 결과

```text
DataIntegrityViolationException
duplicate key value violates unique constraint "uq_approval_step_sequence"
Key (organization_id, request_id, sequence_number) already exists.
```

원본에서 개인정보와 로컬 절대 경로를 제외한 RED 증거는 [red.txt](evidence/INC-001/red.txt)에 있습니다.

## 원인

`ProcurementStore.save`가 새 요청과 DB 복원 요청을 같은 방식으로 처리했습니다. 복원 요청을 저장할 때 기존 managed entity와 자식 ID를 재사용하지 않고 새 UUID를 가진 approval-step entity를 만들었습니다.

## 검토한 해결안

1. 고유 제약 삭제: 중복 승인을 허용하므로 제외했습니다.
2. 저장 전 모든 자식 삭제 후 재삽입: 감사·참조 안정성이 낮고 불필요한 쓰기가 늘어 제외했습니다.
3. 도메인에 JPA 자식 ID 노출: persistence 관심사가 도메인에 침투해 제외했습니다.
4. 기존 managed aggregate를 조직 범위로 읽고 순서별 자식을 갱신: 선택했습니다.

## 적용한 해결

- 새 aggregate와 복원 aggregate의 저장 경로를 분리했습니다.
- 복원 aggregate는 `(organizationId, requestId)`로 managed entity를 읽습니다.
- 승인 단계는 `sequence_number`로 기존 행을 찾아 제자리 갱신합니다.
- 실제 새 단계만 insert합니다.
- 요청의 `@Version`은 JPA가 비교하고, persistence race는 `RequestVersionConflict`로 변환합니다.

## 회귀 검증

GREEN 증거는 [green.txt](evidence/INC-001/green.txt)에 있습니다.

```text
first approval: APPROVED
second stale approval: REQUEST_VERSION_CONFLICT
REQUEST_APPROVED audit count: 1
BUILD SUCCESSFUL
```

## 남은 한계

- 첫 마일스톤은 단일 PostgreSQL database의 optimistic version을 검증합니다.
- 여러 리전의 분산 합의나 이벤트 소싱을 구현한 것으로 표현하지 않습니다.
- 테스트는 실제 동일 시각의 스레드 경합보다, 두 사용자가 같은 version을 읽은 뒤 순차 도착하는 일반적인 stale-client 충돌을 결정적으로 재현합니다.

## 포트폴리오 요약 — 한국어

두 검토자가 같은 version의 요청을 열었을 때 마지막 저장이 앞선 결정을 덮어쓰지 않도록 JPA 낙관적 잠금과 예상 version 계약을 적용했습니다. 구현 중 복원 aggregate의 승인 단계 ID가 사라져 고유 제약을 위반하는 문제를 테스트로 재현했고, 기존 managed entity의 자식 행을 순서별로 갱신하도록 수정했습니다. 첫 승인만 성공하고 오래된 두 번째 승인은 409로 거부되며 감사 로그도 한 건만 남는 것을 PostgreSQL 통합 테스트로 확인했습니다.

## Portfolio summary — English

I used an expected-version contract and JPA optimistic locking so a stale approval cannot overwrite a newer decision. During implementation, the regression test exposed a child-identity bug: rebuilding a restored aggregate generated duplicate approval-step rows. I changed the persistence adapter to update existing managed children by sequence, then verified one successful approval, one stable 409 conflict, and one audit event against PostgreSQL.
