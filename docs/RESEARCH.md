# RESEARCH.md

## Purpose

이 문서는 DMIB를 운영 가능한 에이전트형 서비스로 보기 위해 필요한 개념과 설계 포인트를 정리한다.

핵심 관점:
- Observability
- Correctness
- Recovery
- Repeatable deployment

## DMIB를 에이전트 서비스로 보는 프레임

### Sense
- 외부 데이터 수집
- API 실패 가능성 고려

### Think
- 규칙 기반 해석
- 선택적 LLM 분석

### Act
- Slack 전송
- 필요 시 이메일 전송

### Observe
- `job_run` 기반 실행 결과 저장
- 중복 전송 방지

### Monitor
- Liveness와 Correctness 분리
- 미실행/실패 감지
- 알림 리마인드 정책

### Recover
- 재시도
- 부분 실패 허용
- 운영자 개입 가능성 확보

## Liveness vs Correctness

### Liveness
- 프로세스가 살아 있는가
- Health endpoint가 응답하는가
- 컨테이너가 정상인가

### Correctness
- 오늘 해야 할 실행이 실제로 성공했는가
- Slack 전송까지 완료됐는가
- 실패가 기록되고 감지되는가

DMIB에서는:
- Actuator + Docker healthcheck = Liveness
- `job_run` + monitoring scheduler = Correctness

## 운영 권장사항

### 1. 데이터 재수집과 운영 모니터링은 분리
- 같은 시장 데이터가 하루 중 크게 바뀌지 않는다고 해도
- 잡 실패 여부, 수동 복구 여부, 알림 전송 성공 여부는 계속 확인할 가치가 있다

### 2. 체크 주기와 알림 주기는 분리
- 체크는 10분마다
- 첫 실패는 즉시 알림
- 이후 미해결이면 정각 리마인드

### 3. 배포는 서버 빌드보다 CI 빌드가 유리
- CI에서 artifact 또는 이미지를 만든다
- 서버는 pull과 restart만 담당한다

### 4. 운영 문서는 코드와 같이 관리
- Plan
- Runbook
- Deployment
- Study
- Agent contract

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
