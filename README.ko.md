# FlowOps

FlowOps는 Java 17, Spring Boot 3.5, PostgreSQL, React, TypeScript로 만든 독립 개발 업무 승인 포트폴리오입니다. 첫 공개 준비 범위는 구매 요청을 중심으로 조직별 인증, 서버 금액 계산, 중복 제출 방지, version 충돌, 일관된 API 오류, 실제 브라우저 검증을 연결합니다.

> 표현 경계: 이 저장소는 독립 구현과 로컬 검증 증거입니다. 유료 고객 납품, 정규 근무 경력, 실제 운영 고객 시스템, 결제 플랫폼, 대규모 트래픽 처리 실적으로 표현하지 않습니다.

![FlowOps 로그인](docs/portfolio/screenshots/flowops-login-desktop.png)

![FlowOps 구매 요청](docs/portfolio/screenshots/flowops-request-desktop.png)

## 구현 범위

- 조직 키·이메일·BCrypt 비밀번호·서버 세션·CSRF 기반 로그인
- 다른 조직 데이터의 존재를 노출하지 않는 조직 범위 조회
- 품목 수량과 단가를 이용한 서버 총액 재계산
- 금액별 `REVIEWER → MANAGER → BUDGET_OWNER` 승인 계획
- 조직 범위 멱등성 키와 서버 계산 canonical SHA-256
- JPA version과 `REQUEST_VERSION_CONFLICT` 409 응답
- 임시 저장·제출·승인 감사 로그
- PostgreSQL 16 Testcontainers 기반 Flyway 검증
- React 구매 폼·상세 문서·충돌 안내·모바일 반응형 UI
- 빈 DB Docker Compose 실행과 Playwright 조직 격리 E2E

## 빠른 실행

```bash
docker compose up --build --detach
curl -fsS http://127.0.0.1:4173/actuator/health
```

브라우저에서 `http://127.0.0.1:4173`을 엽니다.

Docker 데모 환경에서만 다음 가상 계정을 설치합니다.

| 조직 키 | 이메일 | 역할 |
|---|---|---|
| `northstar` | `requester@northstar.example.com` | 요청자 |
| `northstar` | `reviewer@northstar.example.com` | 검토자 |
| `acme` | `requester@acme.example.com` | 요청자 |

로컬 데모 비밀번호는 `demo-password`입니다. 실제 배포에서는 `classpath:db/demo`를 활성화하면 안 됩니다.

## 전체 검증

```bash
./scripts/verify-first-slice.sh
./scripts/check-no-browser-auth-storage.sh
./scripts/check-public-safety.sh
```

검증 스크립트는 백엔드 테스트, 프론트 lint·테스트·빌드, 컨테이너 health, Playwright를 실행합니다. 공개 안전 검사는 현재 파일과 전체 Git 이력에서 개인 이메일, 사용자 홈 경로, 자격증명 형태, 내부 수익 작업 파일, 로그, `.env`, 브라우저 trace를 찾습니다.

## 개발 중 재현한 문제

[INC-001](docs/incidents/INC-001-concurrent-approval.md)은 오래된 승인 version을 구현하는 과정에서 승인 단계 자식 행의 ID를 잃어 고유 제약을 위반한 문제를 기록합니다. 실제 고객 장애가 아니라 개발 중 테스트로 재현한 문제이며, RED·원인·대안·수정·GREEN·남은 한계를 구분합니다.

## 현재 한계

- 이번 공개 준비 범위에서 완성한 업무 모듈은 구매 요청입니다.
- 콘텐츠 게시와 현장관리 모듈은 설계만 완료됐습니다.
- 실결제·환불·정산·의료 데이터·산업 장비 연동은 없습니다.
- 공개 프로덕션 배포, 사용자 수, 가동률, 고객 검수, 계약, 지급, 수익 증거는 없습니다.
- CI 파일은 로컬에서 작성했으며 실제 GitHub Actions 실행 전에는 원격 CI 성공으로 주장하지 않습니다.

## 공개와 라이선스

현재는 공개 직전 로컬 마일스톤입니다. 라이선스는 소유자가 명시적으로 선택해 커밋하기 전까지 부여되지 않습니다. GitHub 저장소 생성, 공개 설정, remote 추가, push는 별도의 승인 단계입니다.
