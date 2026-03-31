# AGENT_CONTRACT.md

## Purpose

이 문서는 이 저장소에서 운영하는 각 봇 서비스가 따라야 할 공통 운영 계약을 정의한다.

목표:
- 여러 봇이 생겨도 같은 방식으로 상태를 관측한다
- 운영, 모니터링, 배포 감각을 공통화한다
- 이후 통합 모니터링 서비스와 쉽게 연동한다

## Scope

적용 대상:
- 스케줄 기반 자동화 봇
- 외부 API 수집 서비스
- Slack/Email 전송 서비스
- 이후 통합 모니터링 대상 서비스

현재 구현체:
- DMIB

## 운영 철학

### 1. 실행 가능성보다 운영 가능성이 더 중요하다
- 한 번 성공하는 것보다 반복 실행과 장애 대응이 중요하다

### 2. Liveness와 Correctness를 분리한다
- Liveness: 프로세스와 컨테이너 생존
- Correctness: 오늘 해야 할 실행이 실제로 성공했는지

### 3. 실패는 감지되고 기록되어야 한다
- 실패는 숨기지 않는다
- 기록하고 알리고 복구 가능하게 만든다

## Required capabilities

### Health endpoint
- 최소 `/actuator/health`

### Monitoring endpoint
- 최소 `/internal/monitoring/last-run`

### Run record
- 실행 결과 DB 저장
- 상태, 시간, 에러, hash 등을 기록

### Alerting
- Slack 또는 대체 채널로 결과 전송

### Idempotency
- 같은 결과를 중복 전송하지 않도록 설계

### Reminder policy
- 첫 실패는 빠르게 감지
- 리마인드는 운영자 피로를 고려해 제한

## Status model

현재 최소 상태:
- `SENT`
- `FAILED`

확장 가능 상태:
- `RUNNING`
- `SKIPPED`
- `PARTIAL_SUCCESS`

## Monitoring API contract

Endpoint:
- `GET /internal/monitoring/last-run`

Example:
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
