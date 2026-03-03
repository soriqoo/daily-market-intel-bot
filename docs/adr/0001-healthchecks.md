# ADR 0001 — Healthchecks (Actuator + Docker healthcheck)

## Context
운영에서 “컨테이너가 살아있는지”를 표준 방식으로 판별해야 한다.

## Decision
- Spring Boot Actuator를 활성화하고 `/actuator/health`를 노출한다.
- Docker healthcheck는 `/actuator/health` 기반으로 구성한다.
- Correctness(오늘 실행 여부)는 healthcheck가 아니라 별도 모니터링으로 다룬다.

## Consequences
- Liveness는 도커에서 자동 판별 가능
- Correctness는 job_run + monitor scheduler로 관리
