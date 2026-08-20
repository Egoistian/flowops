# FlowOps — Spring Boot 기반 조직별 구매 요청·승인 시스템

## 배경

업무 요청 시스템은 CRUD 화면만으로 끝나지 않습니다. 같은 요청을 두 명이 동시에 처리하거나, 네트워크 재시도로 제출이 반복되거나, URL의 ID를 바꿔 다른 조직 데이터를 조회하는 문제가 발생할 수 있습니다. FlowOps는 이 문제를 테스트로 먼저 재현하고 해결 근거를 남기기 위해 만든 독립 개발 포트폴리오입니다.

## 구현

- Java 17·Spring Boot 3.5 모듈러 모놀리스
- Spring Security 세션·CSRF와 조직 키 기반 로그인
- PostgreSQL 16·Flyway·JPA `@Version`
- 조직 범위 repository와 외래키
- 금액별 승인 정책과 상태 전환
- 멱등성 키·서버 canonical SHA-256·감사 로그
- React 19·TypeScript·Vite 업무 UI
- Docker Compose·Playwright 실제 브라우저 검증

## 발생한 문제와 해결

1. JPA와 DB 통화 컬럼 타입 불일치 → 기존 migration을 고치지 않고 V003으로 진화.
2. Testcontainers와 Spring context 수명주기 불일치 → JVM 공유 컨테이너로 전체 suite 안정화.
3. 기본 migration의 알려진 데모 계정 → demo Flyway 위치를 분리.
4. 승인 단계 자식 ID 손실 → managed entity의 기존 행을 갱신.
5. 클라이언트가 멱등성 해시를 선택 → 서버가 canonical payload로 계산.
6. SPA 쿠키 CSRF와 XOR handler 불일치 → 실제 쿠키 왕복 테스트와 raw handler 적용.
7. 모바일 표의 음절 단위 줄바꿈 → 우선순위 열과 nowrap 규칙으로 재구성.

## 검증 경계

로컬 자동 테스트, 빈 DB migration, Docker health, Chromium E2E, 실제 데스크톱·모바일 화면을 검증했습니다. 유료 고객 납품, 프로덕션 운영, 대규모 트래픽, 보안 인증, 계약 또는 수익을 주장하지 않습니다.

## 공개 과정에서 발견한 CI 호환성 문제

| 현재 상태 | 결과 |
|---|---|
| 애플리케이션·E2E | 최초 원격 실행부터 통과 |
| 고객·프로덕션 영향 | 없음 |
| 실패한 경계 | CI 안전 검사 명령 호환성 |
| 수정 후 원격 CI | 2회 연속 성공 |
| 공개 `main` | 성공 |

![FlowOps CI 복구 타임라인](screenshots/flowops-ci-recovery-1600x1200.png)

로컬 전체 검증을 통과한 뒤 GitHub에 공개했지만, 첫 Actions 실행은 마지막 공개 안전 검사에서 실패했습니다. 백엔드·프런트엔드·Docker·Playwright E2E는 원격에서도 이미 통과한 상태였습니다. 실패 로그의 첫 원인은 Ubuntu에 없는 `docker-compose` 명령이었습니다. GitHub 러너는 같은 기능을 `docker compose` 플러그인으로 제공했습니다.

### 실패 후 판단

기능 코드나 E2E를 바꾸지 않았습니다. 실패 지점이 애플리케이션 이후의 안전 검사였고, 뒤따른 JSON 오류는 Compose 출력이 비어서 생긴 연쇄 오류였기 때문입니다. GitHub에 `docker-compose`를 추가 설치하는 방법도 검토했지만, 실행환경 가정을 CI 설정으로 숨기는 방식이라 선택하지 않았습니다.

### 해결과 재발 방지

검사 스크립트가 `docker compose`를 먼저 찾고, 없으면 `docker-compose`를 사용하도록 수정했습니다. 어느 명령도 없으면 검사를 건너뛰지 않고 실패합니다. 여기에 plugin-only 가짜 CLI 환경을 추가해, 실제 스크립트가 GitHub와 같은 조건에서 동작하는지 회귀 테스트로 고정했습니다.

수정 후 GitHub Actions가 두 번 연속 통과했습니다. 최종 공개 `main`에서는 백엔드 28개 테스트, 프런트엔드 3개 테스트, Compose E2E, 브라우저 조직 격리, 공개 안전 검사가 모두 성공했습니다. 자세한 판단 과정은 [INC-002](../incidents/INC-002-compose-cli-portability.md)에 기록했습니다.

### 고찰

로컬 성공은 로컬 코드와 로컬 환경의 조합을 증명할 뿐입니다. 원격 CI까지 증명하려면 도구 설치 형태와 운영체제 차이도 테스트 입력으로 다뤄야 합니다. 이번 수정의 핵심은 명령 한 줄을 바꾼 것이 아니라, 두 실행환경을 같은 계약으로 만들고 그 계약이 깨질 때 자동으로 실패하게 만든 것입니다.
