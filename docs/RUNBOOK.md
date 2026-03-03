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
