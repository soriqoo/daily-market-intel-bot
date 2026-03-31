# DMIB

Daily Market Intelligence Bot for collecting U.S. market signals, generating a morning brief, and delivering the result to Slack with operational safeguards.

DMIB is not intended as a one-off automation script. The project is built as a small but production-minded backend service with scheduling, persistence, monitoring, alerting, deployment scripts, and collaboration documents managed together.

## Overview

DMIB runs on a daily schedule and produces a market brief for a Korean investor audience. It collects a small set of market indicators, formats a Slack-friendly summary, optionally appends AI-generated interpretation, stores execution results, and monitors whether the daily run succeeded.

Current data sources:
- FRED
  - S&P 500
  - Nasdaq Composite
  - U.S. 10Y Treasury yield
- FX source
  - USD/KRW

Delivery target:
- Slack Incoming Webhook

## Why This Project Exists

This repository is being used as a practical backend engineering project rather than a toy bot.

Primary goals:
- Build hands-on experience with agent-assisted development
- Practice service-style backend development and operations
- Treat documentation, testing, monitoring, and deployment as first-class concerns
- Create a reusable template for future Slack bots and an integrated monitoring platform

## Project Status

Current status:
- Actively developed and operated on OCI with Docker
- Stable enough to serve as the first bot template in a broader multi-service automation setup
- Still evolving toward registry-based deployment and integrated monitoring

This project is intentionally being used as a practical backend and operations learning platform, with real deployment, alerting, and service documentation practices applied from the beginning.

## Design Principles

- Build small, but operate like a service
- Separate business execution from operational monitoring
- Prefer repeatable workflows over ad-hoc fixes
- Keep documentation, tests, and runtime behavior aligned
- Use AI as an engineering accelerator, not a substitute for reviewable design
- Grow toward reusable contracts that can support multiple bot services

## Key Capabilities

- Daily scheduled execution
- Market data collection from multiple external sources
- Rule-based interpretation for investor-facing summaries
- Optional Gemini-based AI analysis for notable market conditions
- Slack delivery with duplicate payload prevention
- Execution history stored in PostgreSQL
- Internal monitoring endpoint for external health aggregation
- Missed-run and failed-run detection with reminder policy
- Health endpoint and Docker healthcheck support
- Deployment/runbook/documentation tracked in the repository

## Architecture

High-level flow:

1. Scheduler triggers the daily job
2. Data clients fetch market indicators
3. Report service builds a Slack-ready brief
4. Slack notifier sends the message
5. Run store records success or failure
6. Monitoring scheduler checks unresolved failures and sends alerts

Core components:
- [`DailyMarketJob.kt`](D:/Toy_Project/daily-market-intel-bot/src/main/kotlin/com/dbot/dmib/job/DailyMarketJob.kt)
- [`MarketReportService.kt`](D:/Toy_Project/daily-market-intel-bot/src/main/kotlin/com/dbot/dmib/job/MarketReportService.kt)
- [`RunStore.kt`](D:/Toy_Project/daily-market-intel-bot/src/main/kotlin/com/dbot/dmib/store/RunStore.kt)
- [`ExecutionMonitorScheduler.kt`](D:/Toy_Project/daily-market-intel-bot/src/main/kotlin/com/dbot/dmib/monitoring/ExecutionMonitorScheduler.kt)
- [`MonitoringController.kt`](D:/Toy_Project/daily-market-intel-bot/src/main/kotlin/com/dbot/dmib/monitoring/MonitoringController.kt)

## Operational Design

This project intentionally includes operational patterns that are common in service environments:

- Persistent execution history instead of fire-and-forget scheduling
- Idempotency using payload hash comparison
- Partial failure handling for external API fetches
- Monitoring data exposed through a stable internal API
- Alerting policy separated from business execution
- Deployment scripts and runtime configuration separated from source code

Current deployment style:
- OCI Ubuntu instance
- Docker Compose based runtime
- `repo/`, `runtime/`, `data/`, `logs/` directory split on the server

Target deployment style:
- CI-built image
- Registry push
- Server-side `pull + up -d`

## Repository Layout

```text
src/main/kotlin/com/dbot/dmib
  ai/            Gemini integration
  config/        application properties and WebClient config
  datasource/    external API clients
  domain/        market domain models
  job/           scheduled job and report generation
  monitoring/    monitoring API and alert scheduler
  notify/        Slack and mail notification adapters
  store/         JDBC persistence

docs/
  PLAN.md
  RESEARCH.md
  STUDY.md
  RUNBOOK.md
  DEPLOYMENT.md
  AGENT_CONTRACT.md
  CODEX_COLLABORATION.md
```

## Running Locally

Prerequisites:
- JDK 21
- Docker and PostgreSQL, or a reachable PostgreSQL instance

Recommended first check:

```bash
./gradlew test
```

Application configuration is driven by `application.yml` and environment variables.

Important environment variables:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `SLACK_ENABLED`
- `SLACK_WEBHOOK_URL`
- `GEMINI_ENABLED`
- `GEMINI_API_KEY`
- `INTERNAL_TEST_ENABLED`

Notes:
- Internal test endpoints are disabled by default
- AI analysis is optional
- Monitoring endpoints are intended to be consumed by external operational tooling

## Monitoring and Health

Available operational surfaces:
- `GET /actuator/health`
- `GET /internal/monitoring/last-run`

Monitoring behavior:
- The daily job runs on the configured schedule
- Monitoring checks run on a separate schedule
- First unresolved failure is alerted immediately
- Ongoing unresolved failure is reminded on the hour
- Alert state is cleared automatically after recovery

## Testing

The project includes integration tests around:
- run history persistence
- monitoring endpoint behavior
- monitoring reminder policy
- internal test endpoint protection

Run the full test suite with:

```bash
./gradlew test
```

## Documentation

If you are new to the repository, start here:

- [`docs/PLAN.md`](D:/Toy_Project/daily-market-intel-bot/docs/PLAN.md)
- [`docs/RESEARCH.md`](D:/Toy_Project/daily-market-intel-bot/docs/RESEARCH.md)
- [`docs/STUDY.md`](D:/Toy_Project/daily-market-intel-bot/docs/STUDY.md)
- [`docs/RUNBOOK.md`](D:/Toy_Project/daily-market-intel-bot/docs/RUNBOOK.md)
- [`docs/DEPLOYMENT.md`](D:/Toy_Project/daily-market-intel-bot/docs/DEPLOYMENT.md)
- [`docs/AGENT_CONTRACT.md`](D:/Toy_Project/daily-market-intel-bot/docs/AGENT_CONTRACT.md)
- [`docs/CODEX_COLLABORATION.md`](D:/Toy_Project/daily-market-intel-bot/docs/CODEX_COLLABORATION.md)

## Git Workflow

Recommended workflow:

```bash
git switch main
git pull --ff-only
git switch -c feature/<task-name>

# work...
git add .
git commit -m "..."
git push -u origin feature/<task-name>
```

After GitHub merge:

```bash
git switch main
git pull --ff-only
git branch -d feature/<task-name>
```

## Security and Repository Policy

Commit allowed:
- source code
- test code
- public documentation
- deployment templates
- `.env.example`

Never commit:
- real `.env` files
- Slack webhook URLs
- API keys
- OCI credentials
- private SSH keys
- runtime-only override files

## Roadmap

Near-term roadmap:
- standardize a common monitoring contract for multiple bot services
- use DMIB as the first monitored service in an integrated monitoring platform
- improve deployment flow toward registry-based image promotion
- add more reusable operational templates for future bot projects

Possible future extensions:
- economic calendar bot
- OCR operations monitoring bot
- personal workflow or career tracking bot
- centralized monitoring service for all bot projects

## Contributing

This repository is currently maintained as a practical engineering project with strong emphasis on:
- clear commit boundaries
- feature-branch workflow
- operational documentation
- test-backed changes
- agent-assisted collaboration that still leaves a reviewable engineering trail

For repository-specific collaboration rules, see [`docs/CODEX_COLLABORATION.md`](D:/Toy_Project/daily-market-intel-bot/docs/CODEX_COLLABORATION.md).
