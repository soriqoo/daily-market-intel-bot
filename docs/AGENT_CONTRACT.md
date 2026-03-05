# AGENT_CONTRACT.md

## 1. 목적

이 문서는 이 저장소에서 운영되는 자동화 서비스(이하 “Agent”)가 따라야 할 **표준 운영 계약(Contract)** 을 정의한다.

이 계약의 목적은 다음과 같다.

1. 여러 Agent 서비스가 늘어나더라도 **같은 방식으로 상태를 관측**할 수 있게 한다.
2. 서비스별 구현이 달라도, 운영/모니터링/알림/배포 절차는 **일관된 기준**을 따르게 한다.
3. “잘 돌아가는 것처럼 보이지만 실제로는 일을 안 하는” 자동화 시스템을 방지한다.
4. 이후 통합 모니터링 프로젝트를 만들 때, 각 Agent가 **공통된 내부 API와 상태 표현 방식**을 제공하도록 한다.

이 문서는 단순한 개발 규칙이 아니라, **운영 기준서**다.

---

## 2. 적용 범위

이 계약은 다음 범주의 서비스를 대상으로 한다.

- Scheduler 기반 자동 실행 서비스
- 외부 API/데이터 소스를 수집하는 서비스
- AI API(Gemini/OpenAI 등)를 조건부 또는 상시로 호출하는 서비스
- Slack/Email 등 외부 채널로 결과를 전송하는 서비스
- Docker Compose 또는 이후 CI/CD 기반으로 OCI에 배포되는 서비스

현재 1호 Agent는 다음과 같다.

- **DMIB (Daily Market Intelligence Bot)**

향후 추가될 서비스 예시:

- 통합 모니터링 Agent
- 뉴스/리서치 요약 Agent
- 개인 생산성/리마인더 Agent
- 재테크/자산 모니터링 Agent

---

## 3. 핵심 운영 철학

### 3.1 “동작”보다 “운영 가능성”이 더 중요하다
Agent는 한 번 잘 실행되는 것이 아니라, **오랫동안 반복 실행되면서 문제를 감지하고 회복 가능해야 한다.**

즉, 아래가 모두 갖춰져야 한다.

- 실행 가능성
- 상태 관측 가능성
- 실패 감지 가능성
- 중복 방지
- 부분 실패 허용 여부 명시
- 알림 채널 확보
- 운영 절차(runbook) 존재

### 3.2 Liveness와 Correctness를 분리한다
운영에서 가장 흔한 실수는 “컨테이너가 살아 있으니 정상”이라고 판단하는 것이다.

Agent 운영에서는 반드시 아래 두 개를 구분해야 한다.

#### Liveness
- 프로세스/컨테이너가 살아 있는가?
- HTTP health endpoint가 응답하는가?
- 도커 healthcheck는 통과하는가?

#### Correctness
- 오늘/이번 스케줄에 실제로 해야 할 일을 했는가?
- Slack/Email 전송까지 성공했는가?
- 외부 데이터 수집 결과가 비정상이라면 실패로 기록되었는가?

이 계약에서는:
- **Actuator health / docker healthcheck** 는 Liveness
- **job_run / monitoring scheduler / monitoring API** 는 Correctness를 다룬다.

### 3.3 실패는 없앨 수 없으므로, 감지/격리/알림을 설계한다
외부 API가 포함된 Agent는 반드시 실패한다.

예:
- 네트워크 타임아웃
- DNS/TLS 문제
- API 정책 변경
- 응답 포맷 변경
- 스케줄러 미실행
- Slack webhook 오류
- AI 응답 포맷 불안정

따라서 목표는 “실패하지 않게”가 아니라:
- 빨리 감지하고
- 영향을 줄이고
- 운영자에게 알려주고
- 재발 방지에 반영하는 것

---

## 4. Agent가 반드시 가져야 하는 최소 기능

모든 Agent는 최소한 아래 기능을 가져야 한다.

### 4.1 Health endpoint
- 최소 `/actuator/health`
- docker healthcheck에서 참조 가능해야 함
- 기본적으로 `127.0.0.1` 바인딩으로만 운영하고 외부 공개는 지양

### 4.2 Monitoring endpoint
- 최소 `/internal/monitoring/last-run`
- 마지막 실행 결과를 표준 JSON 형태로 반환
- 이 엔드포인트는 운영/통합 모니터링 용도이며, public exposure 대상이 아님

### 4.3 Run 기록 저장
- DB 테이블에 실행 결과를 기록해야 함
- 최소 상태:
    - 언제 실행했는가
    - 성공/실패 여부
    - 에러 요약
    - 최종 전송 시각
    - payload hash(중복 방지용)

### 4.4 Slack 알림(또는 동급 채널)
- 성공 리포트 또는 실패 경고를 외부 채널로 전달 가능해야 함
- 채널 기반 운영은 “사람이 실제로 상태를 본다”는 점에서 매우 중요하다

### 4.5 Idempotency
- 같은 스케줄에서 같은 payload를 중복 전송하지 않도록 설계
- 해시 기반 또는 run-date 기반 중복 방지 허용

### 4.6 Monitoring throttle
- 같은 장애/같은 미실행 경고를 반복적으로 보내지 않도록 억제
- 권장: DB 기반 throttle + fallback(메모리)

---

## 5. 표준 상태 모델(Status Model)

Agent는 최소한 아래 상태를 표현할 수 있어야 한다.

### 5.1 최소 상태 집합
- `SENT`
- `FAILED`

### 5.2 권장 확장 상태
- `RUNNING`
- `SKIPPED`
- `THROTTLED`
- `PARTIAL_SUCCESS`

현재 DMIB는 `SENT / FAILED` 중심으로 동작한다.  
향후 통합 모니터링 및 더 정밀한 운영을 위해, 상태 모델은 확장 가능해야 한다.

### 5.3 상태 의미
#### SENT
- 리포트/알림 전송까지 성공
- 운영 관점에서 “이번 런은 정상 완료”

#### FAILED
- 실행 결과를 사용자/채널에 정상 전달하지 못했거나,
- 최소 성공 조건(예: 유효한 metrics 1개 이상)을 만족하지 못함

#### SKIPPED
- 정책에 의해 실행을 생략함
- 예: 이미 같은 payload를 보냄(idempotency), 트리거 불충족

#### PARTIAL_SUCCESS
- 일부 외부 API 실패가 있었지만, 최소 유효 결과는 전달됨

---

## 6. 표준 Monitoring API 계약

### 6.1 엔드포인트
`GET /internal/monitoring/last-run`

### 6.2 목적
- 통합 모니터링 서비스가 Agent의 가장 최근 실행 상태를 수집하기 위함
- 운영자가 “오늘 정상 실행됐는지”를 빠르게 확인하기 위함

### 6.3 응답 예시 (표준형)
```json
{
  "service": "dmib",
  "environment": "prod",
  "timezone": "Asia/Seoul",
  "lastRunDate": "2026-02-26",
  "status": "SENT",
  "sentAt": "2026-02-26T08:00:03+09:00",
  "error": null
}
