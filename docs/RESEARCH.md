# 설계 및 조사 노트

## 문서 목적

이 문서는 DMIB를 운영 가능한 에이전트형 서비스로 보기 위해 필요한 개념과 설계 판단을 정리한다.

핵심 관점은 아래와 같다.
- Observability
- Correctness
- Recovery
- Repeatable deployment
- Reusable service template

## DMIB를 에이전트 서비스로 보는 프레임

### Sense
- 외부 API에서 시장 데이터를 수집한다.
- 데이터 소스는 실패할 수 있으므로 부분 실패 허용이 필요하다.

### Think
- 규칙 기반 해석을 우선 적용한다.
- 특정 조건에서는 LLM 기반 보조 해석을 붙인다.

### Act
- Slack으로 결과를 전송한다.
- 필요 시 이메일 등 다른 채널로도 확장할 수 있다.

### Observe
- 실행 결과를 DB에 남긴다.
- payload hash로 중복 전송을 억제한다.

### Monitor
- 프로세스 상태와 비즈니스 성공 여부를 분리해서 본다.
- 실패는 즉시 감지하고, 해결 전까지 리마인드한다.

### Recover
- 수동 재실행 가능성을 고려한다.
- 부분 실패 상황에서도 가능한 범위의 결과를 남긴다.
- 운영자가 로그와 상태 기록을 보고 원인을 좁힐 수 있어야 한다.

## 이미 내린 설계 판단

### 1. 데이터 재수집과 운영 모니터링은 분리한다
같은 시장 데이터는 하루 안에 크게 변하지 않을 수 있다. 하지만 아래 상태는 계속 확인할 가치가 있다.
- 오늘 잡이 실제로 성공했는가
- Slack 전송이 정상 완료됐는가
- 수동 복구가 되었는가
- 아직 unresolved failure 상태인가

그래서 DMIB는 데이터를 다시 fetch하기보다 `job_run` 상태를 기준으로 모니터링한다.

### 2. 체크 주기와 알림 주기를 분리한다
실패 감지는 빨라야 하지만, Slack 알림은 과하면 안 된다.
현재 DMIB의 정책은 아래와 같다.
- 상태 체크: 10분마다
- 첫 실패: 즉시 알림
- 미해결 상태: 정각 리마인드

### 3. 내부 테스트 엔드포인트는 기본 비활성화한다
운영 서버에서 테스트용 실행 API가 항상 열려 있으면 보안과 운영 리스크가 커진다.
따라서 다음 원칙을 적용했다.
- 기본 비활성
- 명시적 설정 시에만 활성화
- 동작은 테스트 코드로 고정

### 4. 현재 배포는 학습용으로 충분하지만, 장기적으로는 image pull 기반이 맞다
현재는 OCI 접근 이슈 때문에 `repo pull + compose up` 형태를 유지하지만, 목표는 아래와 같다.
- CI에서 이미지 생성
- Registry push
- 서버는 pull과 실행만 수행

## 통합 모니터링 서비스로 확장할 때의 원칙

DMIB는 향후 여러 봇 중 하나가 될 예정이므로, 중앙 모니터링 서비스와의 연동을 고려해야 한다.

### 권장 구조
- 각 봇은 자체 실행과 자체 Slack 전송을 담당한다.
- 중앙 모니터링 서비스는 각 봇의 상태 API를 조회한다.
- 판단 기준은 Slack 메시지 자체가 아니라 `last-run`, `health`, `error`, `sentAt` 같은 공통 상태 값이다.

### 초기 버전 권장 기술
- Spring Boot
- PostgreSQL
- HTTP polling
- Slack notifier

### 나중에 검토할 기술
- Redis: 중복 알림 제어, 캐시, 분산락이 필요할 때
- Kafka: 여러 서비스의 상태 이벤트를 비동기로 수집할 때
- SSE/WebSocket: 중앙 대시보드를 만들 때
- Testcontainers: PostgreSQL/Redis 기반 테스트를 실전형으로 고도화할 때

## 기술 도입 타이밍 메모

### Redis
도입 시점:
- 다중 인스턴스 실행
- 알림 중복 억제 강화
- 상태 캐시 필요
- 분산락 필요

### Kafka
도입 시점:
- 서비스 수가 늘어 상태 이벤트를 비동기로 모으고 싶을 때
- 중앙 모니터링이 polling보다 event-driven 구조가 나아질 때

### WebSocket 또는 SSE
도입 시점:
- Slack만으로 부족하고 브라우저 대시보드를 함께 운영할 때
- 우선순위는 WebSocket보다 SSE가 더 높다

### Testcontainers
도입 시점:
- H2보다 PostgreSQL/Redis 실제 동작 검증이 중요해질 때
- 통합 모니터링 서비스처럼 인프라 의존성이 늘어날 때

## 참고 자료

- Spring Boot Actuator
  - https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
  - https://spring.io/blog/2020/03/25/liveness-and-readiness-probes-with-spring-boot
- Docker GitHub Actions
  - https://docs.docker.com/build/ci/github-actions/
  - https://github.com/docker/build-push-action
  - https://github.com/docker/login-action
- Oracle OCIR
  - https://docs.oracle.com/en-us/iaas/Content/Registry/Tasks/registrypushingimagesusingthedockercli.htm
  - https://docs.oracle.com/en-us/iaas/Content/Registry/Tasks/registrypullingimagesusingthedockercli.htm
