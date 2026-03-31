# DMIB - Daily Market Intelligence Bot

DMIB는 매일 정해진 시간에 시장 데이터를 수집하고, 규칙 기반 해석과 선택적 AI 요약을 더해 Slack으로 전송하는 자동화 봇입니다.

이 저장소의 목표는 단순한 사이드 프로젝트가 아니라, 실제 서비스 운영을 염두에 둔 백엔드 자동화 시스템을 만드는 것입니다.

## What it does

- 스케줄 기반 데일리 브리프 생성
- 시장 데이터 수집
  - FRED: S&P 500, Nasdaq, US 10Y
  - 환율: USD/KRW
- 규칙 기반 해석
- 선택적 AI 분석 섹션 추가
- Slack Webhook 전송
- PostgreSQL 기반 실행 기록 저장
- 운영 기능
  - `/actuator/health`
  - Docker healthcheck
  - 미실행/실패 감지
  - 중복 알림 억제
  - 표준 배포 스크립트

## Docs

- 연구 및 운영 설계: `docs/RESEARCH.md`
- 구현 계획: `docs/PLAN.md`
- 학습 메모: `docs/STUDY.md`
- 운영 Runbook: `docs/RUNBOOK.md`
- 배포 정책: `docs/DEPLOYMENT.md`
- 에이전트 공통 계약: `docs/AGENT_CONTRACT.md`
- 협업 규칙: `docs/CODEX_COLLABORATION.md`

## Repository policy

Commit OK:
- 코드
- 설정 템플릿
- 공개 가능한 문서
- `.env.example`

Never commit:
- `.env`
- 실제 Slack webhook URL
- Gemini/OpenAI API key
- OCI auth token / SSH private key
- 운영 전용 override 파일

## Local development

Prerequisites:
- JDK 21
- Gradle wrapper

```bash
./gradlew clean test
```

## Branch workflow

```bash
git switch main
git pull --ff-only
git switch -c feature/<task-name>

# work...
git add .
git commit -m "..."
git push -u origin feature/<task-name>
```
