# RESEARCH.md — DMIB 운영 완전체/Agent 자동화 아키텍처 연구 노트

## 0. 이 문서의 목적
이 프로젝트는 “작동하는 봇”이 아니라, **운영 가능한 자동화 시스템(Agent형)**을 만들기 위한 훈련장이다.

- 자동화의 핵심은 “기능 구현”이 아니라 **운영 가능성(Observability + Correctness + Recovery)** 이다.
- 본 문서는 DMIB를 기반으로, 이후 여러 Agent 컨테이너를 추가해도 흔들리지 않는 운영 모델을 정리한다.

---

## 1. DMIB를 Agent형 시스템으로 바라보는 프레임
Agent를 거창하게 정의하지 말고, 운영 가능한 자동화의 구성요소로 쪼개면 이해가 쉽다.

### 1) Sense (관측/수집)
- 외부 데이터(FRED, FX API 등)를 수집
- 실패 가능성이 높음: 네트워크/서버/정책/포맷 변화

### 2) Think (해석/추론)
- 룰 기반(해석 문장/체크리스트)
- 조건부 LLM(Gemini) 호출
- 비용/품질 트레이드오프가 존재

### 3) Act (행동/전달)
- Slack webhook 전송
- (옵션) Gmail 전송

### 4) Observe (기록)
- job_run 테이블로 실행 결과 기록
- idempotency(중복 전송 방지) 지원

### 5) Monitor (감시)
- “프로세스 살아있나(liveness)” + “오늘 제대로 실행했나(correctness)” 분리
- 미실행 감지 + throttle(중복 경고 억제)

### 6) Recover (복구)
- 재시도(backoff)
- 부분 실패 허용(가능한 데이터만으로 보고)

---

## 2. 운영 관점의 핵심 구분: Liveness vs Correctness
### Liveness = 살아있나?
- 컨테이너가 죽었는지, 앱이 응답하는지 확인
- 도커/오케스트레이터가 재시작 같은 자동 복구를 할 때 기준이 됨

### Correctness = 오늘 해야 할 일을 했나?
- 08:00 리포트가 실제로 전송됐는지(성공 여부)
- “앱이 살아있는데도” 스케줄이 안 도는 문제가 실제 운영에서 흔함

DMIB는 이 둘을 분리했다:
- Actuator + Docker healthcheck = Liveness
- job_run + 미실행 감지 스케줄러 + throttle = Correctness

---

## 3. Spring Boot Actuator: Health의 의미와 함정
Spring Boot는 liveness/readiness 그룹을 제공한다.
하지만 “liveness는 외부 의존성(DB, 외부 API)에 의존하면 안 된다”는 원칙이 있다.
외부 시스템 문제로 liveness가 실패하면 재시작이 연쇄적으로 발생해 더 큰 장애를 만들 수 있다.

- Spring Boot actuator health group(공식): https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- Spring liveness/readiness 설명: https://spring.io/blog/2020/03/25/liveness-and-readiness-probes-with-spring-boot

운영 권장:
- Docker healthcheck는 liveness 위주(UP)
- DB/외부 API는 correctness/monitoring에서 감지 → 알림/조치

---

## 4. Docker Compose 운영 패턴: repo 파일과 runtime 파일 분리
### 원칙
- GitHub(public)에는 “템플릿/안전한 기본값”만 올린다.
- 운영 환경 차이(포트 바인딩, override)는 서버에서만 유지한다.
- 비밀(.env)은 절대 커밋하지 않는다.

### 권장 파일 분리
- repo/docker-compose.yml: 공통(안전)
- repo/docker-compose.override.yml: 서버 전용(커밋 금지)
- runtime/.env: 서버 전용(커밋 금지)

---

## 5. 알림(Alerts)과 Alert Fatigue
### 왜 throttle이 필요한가?
운영에서 “알림이 많은 시스템”은 결국 무시된다.
그래서 같은 장애는 일정 시간 동안 1번만 알리도록 한다.

DMIB는 60분 throttle을 도입했다.
- DB throttle: 재시작해도 중복 경고 억제
- fallback: DB가 죽으면 메모리 기반 최소 억제

---

## 6. 배포의 정석: Immutable Artifact (CI 빌드 → Registry → 서버 pull)
### 문제: 서버에서 git pull + build
- 서버가 빌드 환경이 됨(빌드/네트워크/권한/캐시 문제)
- 배포 결과가 “그때그때 달라질 수 있음”

### 해법: CI에서 이미지 빌드하고 서버는 pull만
- GitHub Actions에서 이미지 빌드/테스트 후 OCIR에 push
- OCI 서버는 docker login을 한 번 해두고
- `docker compose pull && docker compose up -d`로 배포

Docker build/push 액션:
- https://github.com/docker/build-push-action
- https://github.com/docker/login-action
  멀티아치 빌드 개요:
- https://docs.docker.com/build/ci/github-actions/multi-platform/

---

## 7. OCIR(OCI Container Registry) 인증/권한 개념
OCIR 로그인은 auth token 기반이 일반적이다.
- docker login / push 공식: https://docs.oracle.com/en-us/iaas/Content/Registry/Tasks/registrypushingimagesusingthedockercli.htm
- pull 공식: https://docs.oracle.com/en-us/iaas/Content/Registry/Tasks/registrypullingimagesusingthedockercli.htm
- username 형식(특히 federated): 위 pull 문서 참고
- Oracle GitHub Action: https://github.com/oracle-actions/login-ocir

---

## 8. 확장 로드맵: 통합 모니터링으로 가는 길
DMIB가 운영 완전체가 되면 다음 단계는 “여러 Agent 컨테이너”를 운영하는 통합 모니터링이다.

통합 모니터링이 수집해야 할 최소 계약(Contract):
- 각 서비스가 /internal/monitoring/last-run 을 제공
- 통합 모니터링은 SLA 위반(오늘 실행/최근 성공)만 체크하면 된다.

---

## 9. 참고자료(권장 우선순위)
1) Spring Boot Actuator (공식)
- https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- https://spring.io/blog/2020/03/25/liveness-and-readiness-probes-with-spring-boot

2) Docker 공식 GitHub Actions
- https://docs.docker.com/build/ci/github-actions/
- https://github.com/docker/build-push-action
- https://github.com/docker/login-action

3) Oracle OCIR(공식)
- https://docs.oracle.com/en-us/iaas/Content/Registry/Tasks/registrypushingimagesusingthedockercli.htm
- https://docs.oracle.com/en-us/iaas/Content/Registry/Tasks/registrypullingimagesusingthedockercli.htm
- https://github.com/oracle-actions/login-ocir

---

## 10. CI as a Guardrail (GitHub Actions CI-only)
CI는 “배포 자동화” 이전의 단계로, 잘못된 변경이 main에 들어가는 것을 막는 보호장치(guardrail) 역할을 한다.

DMIB는 현재 다음 CI를 사용한다.
- Gradle test + bootJar
- Docker build validation (arm64, thin packaging)

핵심 교훈:
- CI는 GitHub에서 push/PR 시점에 실행되며, 운영 서버(OCI)와는 직접 관계가 없다.
- workflow 파일만 수정한 경우 OCI에 배포할 필요가 없다.
- CI는 Liveness/Correctness가 아니라 “변경이 안전한지”를 사전에 검증하는 레이어다.

### 왜 패키징 분리가 중요한가?
초기에는 Docker 내부에서 Gradle 빌드를 수행하여 arm64 검증이 10분 이상 걸렸다.
이후 CI에서:
1. bootJar 생성
2. artifact 업로드
3. thin Docker packaging (`Dockerfile.ci`)
   으로 분리하면서 Docker build validation 시간이 크게 줄었다.

이는 실무적으로 흔한 패턴이다.
- Build(artifact 생성)와 Package(이미지화)를 분리하면
- CI 시간과 실패 원인 분리 품질이 크게 좋아진다.

### 다음 운영 규율
- main은 protected branch로 관리
- required checks가 통과해야만 merge
- 직접 main push는 지양 또는 금지

---

## 11. Timezone correctness in scheduled agents
Scheduler가 특정 timezone(예: Asia/Seoul) 기준으로 돌더라도, 코드에서 `LocalDate.now()`를 JVM 기본 timezone(예: UTC)으로 계산하면 runDate가 하루 어긋날 수 있다.

DMIB에서 실제로 발생한 문제:
- 08:00 KST에 리포트는 정상 전송됨
- 그러나 runDate가 UTC 기준 전날로 저장됨
- monitoring scheduler는 “오늘 run이 없다”고 판단하여 false positive 경고를 전송함

교훈:
- 스케줄 cron의 zone과 날짜 계산(`LocalDate.now(ZoneId.of(app.timezone))`)은 반드시 동일한 timezone 기준을 써야 한다.
