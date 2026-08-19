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
