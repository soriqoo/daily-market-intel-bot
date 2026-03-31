# 봇 서비스 공통 계약

## 문서 목적

이 문서는 이 저장소에서 운영하는 봇 서비스가 공통으로 따라야 할 최소 운영 계약을 정의한다.
이 계약은 이후 통합 모니터링 서비스와 여러 Slack 봇 프로젝트를 연결하기 위한 기준선 역할을 한다.

현재 기준 구현체:
- DMIB

## 적용 대상

- 스케줄 기반 자동화 봇
- 외부 API 수집 후 Slack 또는 메일로 결과를 전송하는 서비스
- 이후 통합 모니터링 대상이 될 모든 개인 서비스

## 핵심 원칙

### 1. 한 번 동작하는 것보다 운영 가능한 것이 더 중요하다
- 반복 실행 가능해야 한다.
- 실패를 감지하고 복구할 수 있어야 한다.
- 운영자가 상태를 설명할 수 있어야 한다.

### 2. Liveness와 Correctness를 분리한다
- Liveness: 프로세스와 컨테이너가 살아 있는가
- Correctness: 오늘 해야 할 실행이 실제로 성공했는가

### 3. 실패는 기록되고 관측 가능해야 한다
- 실패를 숨기지 않는다.
- 상태와 에러를 남긴다.
- 모니터링 시스템이 읽을 수 있어야 한다.

## 필수 기능

모든 봇 서비스는 가능하면 아래 기능을 제공한다.

### Health endpoint
- 최소 `GET /actuator/health`

### Monitoring endpoint
- 최소 `GET /internal/monitoring/last-run`

### Run record
- 실행 날짜
- 상태
- 전송 시간
- payload hash
- 에러 메시지

### Alerting
- Slack 또는 대체 채널 전송 기능

### Idempotency
- 같은 결과를 중복 전송하지 않도록 설계

### Reminder policy
- 첫 실패는 빠르게 감지
- 리마인드는 운영 피로를 고려해 제한

## 최소 상태 모델

현재 기본 상태:
- `SENT`
- `FAILED`

향후 확장 가능 상태:
- `RUNNING`
- `SKIPPED`
- `PARTIAL_SUCCESS`

## 공통 모니터링 API 계약

### Endpoint
- `GET /internal/monitoring/last-run`

### 최소 응답 필드

```json
{
  "service": "dmib",
  "environment": "prod",
  "timezone": "Asia/Seoul",
  "lastRunDate": "2026-03-31",
  "status": "SENT",
  "sentAt": "2026-03-31T08:00:03+09:00",
  "error": null
}
```

### 필드 의미
- `service`: 서비스 식별자
- `environment`: 실행 환경
- `timezone`: 기준 시간대
- `lastRunDate`: 마지막 실행 대상 날짜
- `status`: 마지막 실행 상태
- `sentAt`: 실제 전송 완료 시각
- `error`: 마지막 실패 원인

## 통합 모니터링 서비스와의 연계 원칙

중앙 모니터링 서비스는 각 봇의 Slack 메시지를 파싱하지 않는다.
대신 아래를 기준으로 상태를 판단한다.
- `health`
- `last-run`
- 응답 지연 또는 미응답
- 에러 필드 유무

즉, 봇은 “결과를 보내는 서비스”이고, 중앙 모니터링은 “그 서비스가 정상 동작 중인지 감시하는 서비스”로 역할을 분리한다.

## 배포 및 운영 규칙

- 배포 가능한 문서와 실행 절차를 저장소에 남긴다.
- 비밀값은 `runtime/.env` 같은 별도 영역에서 관리한다.
- 테스트용 내부 엔드포인트는 기본 비활성화한다.
- 운영 중 수동 조치는 기록 가능한 형태로 남긴다.
