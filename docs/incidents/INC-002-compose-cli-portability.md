# INC-002 — 공개 CI에서 드러난 Compose CLI 호환성 문제

## 분류

- 유형: 공개 과정에서 재현된 CI 실행환경 호환성 문제
- 최초 원격 실행: GitHub Actions `32314882027`
- 최초 커밋: `f857462f89584a19dcfeb49883d7df24e9a503ea`
- 최종 검증 실행: GitHub Actions `32315607695`
- 최종 검증 커밋: `2297bce31a1d0fa9820b98378673e78eb4a8003c`
- 고객·프로덕션 영향: 없음

## 상황

FlowOps는 공개 전에 로컬에서 백엔드 테스트, 프런트엔드 검사, Docker Compose, Playwright 조직 격리 E2E, 개인정보 검사를 통과했다. 공개 직전에는 프런트엔드 포트를 `127.0.0.1:4173`으로 제한하고, 렌더링된 Compose 설정이 이 조건을 지키는지 확인하는 `check-local-demo-network.sh`를 추가했다.

로컬에서는 독립 실행 파일인 `docker-compose` 5.5.0을 사용했다. 최초 GitHub Actions 실행의 Ubuntu 러너에는 같은 기능이 Docker CLI 플러그인인 `docker compose` 형태로만 제공됐다.

## 관찰된 실패

최초 원격 실행에서 다음 단계는 통과했다.

- backend tests;
- frontend verification;
- Playwright browser installation;
- Docker Compose startup and E2E.

마지막 `Public-safety checks`에서만 실패했다.

```text
./scripts/check-local-demo-network.sh: line 7: docker-compose: command not found
```

뒤따른 Ruby JSON 오류는 Compose 출력이 비어 발생한 2차 증상이었다. JSON 파서를 수정하는 것은 근본 원인을 해결하지 못한다.

## 실패 후 고찰

### 1. 로컬 성공은 원격 성공의 증거가 아니었다

로컬 전체 검증은 코드와 로컬 실행환경의 조합을 증명했다. GitHub의 Ubuntu 환경까지 증명한 것은 아니었다. 특히 Compose가 같은 제품이라도 `docker-compose`와 `docker compose`라는 두 명령 표면으로 제공될 수 있다는 차이를 안전 게이트가 흡수하지 못했다.

### 2. 실패한 단계가 중요했다

애플리케이션·데이터베이스·브라우저 E2E는 이미 원격에서 통과했다. 따라서 구매 요청 기능이나 조직 격리 코드를 바꾸면 문제의 계층을 잘못 짚는 셈이었다. 실패는 보안 검사 스크립트가 Compose 설정을 읽기 전에 발생했다.

### 3. 첫 오류와 연쇄 오류를 구분해야 했다

`docker-compose: command not found`가 최초 오류였고, 빈 입력을 읽은 JSON 파서 실패는 결과였다. 파서에 빈 입력 예외 처리를 추가하는 것만으로는 안전 검사가 실행되지 않은 상태를 성공으로 오인할 수 있었다.

### 4. 수동 수정만으로는 재발을 막지 못했다

스크립트의 명령 한 줄을 `docker compose`로 바꾸면 GitHub에서는 통과하지만, 독립 명령만 설치된 기존 로컬 환경을 깨뜨릴 수 있다. 두 환경을 모두 계약으로 다루는 회귀 테스트가 필요했다.

## 검토한 해결안

### A. GitHub Actions에 `docker-compose`를 별도 설치

기각했다. 검사 스크립트의 불필요한 환경 가정을 CI 설정으로 숨기고, 설치 시간과 외부 의존성만 늘린다.

### B. 명령을 `docker compose`로 완전히 교체

기각했다. GitHub에는 맞지만 독립 `docker-compose`만 있는 로컬 환경과의 호환성을 잃는다.

### C. 스크립트가 두 명령 표면을 감지

선택했다. 표준 플러그인 형태를 우선하고, 없으면 독립 명령으로 대체한다. 둘 다 없으면 안전 검사를 실행할 수 없으므로 명시적으로 실패한다.

```bash
if docker compose version >/dev/null 2>&1; then
  compose=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  compose=(docker-compose)
else
  printf 'FAIL docker-compose-unavailable\n' >&2
  exit 1
fi
```

## 회귀 테스트

`scripts/test/fixtures/docker`는 `docker compose`만 제공하는 제한된 가짜 CLI다. 테스트는 PATH에서 실제 `docker-compose`를 제거한 뒤 네트워크 안전 게이트를 실행한다.

수정 전:

```text
docker-compose: command not found
```

수정 후:

```text
PASS compose-plugin-compatibility
PASS local-demo-network
```

테스트는 명령 문자열을 grep하지 않는다. plugin-only 환경에서 실제 스크립트를 실행하고 종료 상태와 결과를 확인한다.

## 검증 결과

호환 수정 후 원격 실행 `32315315195`가 성공했다. README 배지와 공개 검증 기록을 추가한 최종 커밋에서는 `32315607695`가 다시 성공했다.

최종 실행 단계:

- backend tests: success;
- frontend verification: success;
- browser installation: success;
- Compose and E2E: success;
- public-safety checks: success.

로컬 HEAD, `origin/main`, 원격 `refs/heads/main`은 모두 `2297bce31a1d0fa9820b98378673e78eb4a8003c`로 일치했다.

## 남은 한계

- 이 사례는 GitHub Actions 공개 과정의 CI 문제이며 고객 운영 장애가 아니다.
- 실제 프로덕션 배포, 장애 시간, 사용자 영향, 복구 시간 목표를 증명하지 않는다.
- GitHub는 일부 Actions의 Node.js 20 런타임과 `actions/setup-java@v4` 사용에 비차단 유지보수 경고를 표시했다.
- 현재 검증은 macOS 로컬 환경과 GitHub Ubuntu 러너를 다룬다. 다른 CI 제품과 Windows 러너까지 일반화하지 않는다.

## 재사용한 원칙

1. 실패 단계가 속한 계층부터 확인한다.
2. 최초 오류와 연쇄 오류를 분리한다.
3. 한 환경을 고치면서 다른 환경을 깨뜨리지 않는다.
4. 환경 차이를 회귀 테스트의 입력으로 만든다.
5. 수정된 최종 커밋 자체에서 원격 검증을 다시 수행한다.
