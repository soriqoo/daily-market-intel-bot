# ADR 0002 — Missed/Failed run monitoring + Throttle

## Context
미실행/실패 감지는 필요하지만, 동일 장애 알림이 반복되면 Slack 채널이 망가진다.

## Decision
- 미실행/실패 감지 스케줄러를 별도로 둔다.
- throttle window는 60분으로 시작한다.
- throttle은 DB 기반(monitor_alert)으로 구현하고, DB 장애 시 메모리 fallback을 둔다.

## Consequences
- 운영 알림 신뢰도 상승
- 재기동/배포에도 중복 알림 억제 유지
