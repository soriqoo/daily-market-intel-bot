# DMIB — Daily Market Intelligence Bot

DMIB는 **매일 정해진 시간에 시장 데이터를 수집하고**, (옵션) **AI 요약을 추가**하여 **Slack으로 아침 브리핑을 보내는 자동화 봇**입니다.  
목표는 “작동하는 봇”이 아니라, **운영 가능한(Production-grade) 자동화 시스템**을 만드는 것입니다.

---

## What it does

- Scheduler 기반 자동 실행(일일 브리핑)
- Market data 수집
    - FRED (S&P 500, Nasdaq, US10Y 등)
    - 환율(USD/KRW)
- 룰 기반 해석(초보자 친화 “Interpretation / Action Items”)
- (옵션) Gemini 조건부 호출로 AI 분석 섹션 추가
- Slack Webhook 알림
- PostgreSQL에 실행 결과 기록(job_run)
- 운영 기능
    - Actuator health (`/actuator/health`)
    - Docker healthcheck
    - 미실행/실패 감지 + throttle(중복 알림 억제)
    - 배포 표준화 스크립트(`dmib.sh deploy`)

---

## Docs

- Research / 운영 이론: `docs/RESEARCH.md`
- 구현 계획: `docs/PLAN.md`
- 학습 요약 + 질문 템플릿: `docs/STUDY.md`
- 운영 Runbook: `docs/RUNBOOK.md`
- ADR(설계 결정 기록): `docs/adr/`

---

## Repository policy (Public-safe)

✅ Commit OK
- 코드/설정 템플릿
- docs/ 문서 (비밀 제외)
- `.env.example` 같은 예시 파일

❌ NEVER commit
- `.env` (실제 운영 값)
- Slack webhook URL
- Gemini/OpenAI API Key
- OCI Auth Token / SSH private key
- `docker-compose.override.yml` (서버 전용 override)

---

## Local development

Prereqs:
- JDK 21
- Gradle wrapper

```bash
./gradlew clean test
