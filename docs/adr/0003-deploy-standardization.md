# ADR 0003 — Deployment standardization (dmib.sh deploy)

## Context
운영 서버에서 수동 명령은 실수/편차를 만든다.

## Decision
- deploy/restart/logs/health 등을 스크립트로 표준화한다.
- deploy는 `git pull --ff-only`를 사용하여 서버에서 merge/충돌을 방지한다.
- 향후 CI/CD 전환 시 서버는 pull-only로 변경한다.

## Consequences
- 배포 재현성 향상
- 운영 실수 감소
