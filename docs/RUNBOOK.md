# DMIB Runbook (Daily Market Intelligence Bot)

## Purpose
- Daily scheduled market brief sent to Slack (and optional AI analysis via Gemini)
- Designed to be robust: partial failures allowed, idempotent sending, monitoring alerts

## Environments
- **OCI Ubuntu** runs Docker Compose
- Secrets live only in: `{your_project_path}/runtime/.env`
- OCI-only overrides live in: `{your_project_path}/repo/docker-compose.override.yml` (NOT committed)

## Standard Commands (OCI)
> Alias `dmib` points to `{your_project_path}/runtime/dmib.sh`

### Deploy (safe)
- Pull latest code (fast-forward only)
- Rebuild & restart containers
- Wait for health (best-effort)

```bash
dmib deploy
```

## CI / GitHub Actions
### What CI validates
- Gradle test + bootJar
- Docker build validation (arm64, thin packaging)

### Important rule
Workflow-only changes (`.github/workflows/*.yml`) do NOT require OCI deploy.
GitHub Actions runs on GitHub infrastructure when code is pushed.

### When OCI deploy is required
- Application code changes
- Dockerfile / compose changes
- Runtime behavior changes
- Config affecting app behavior

### Branch workflow
Recommended:
- Create a feature branch
- Open PR to main
- Wait for required checks
- Merge after CI passes


## Git workflow after branch protection
### Rule
Do not push directly to `main`.

### Standard flow
```bash
git switch main
git pull --ff-only
git switch -c feature/<task-name>

# work...
git add .
git commit -m "..."
git push -u origin feature/<task-name>
```

