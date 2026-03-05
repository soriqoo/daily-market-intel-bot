# PLAN.md — DMIB 구현/반영 계획서 (운영 완전체 + CI/CD 전환)

## 0) 현재 상태(Completed)
- A1: Actuator + Docker healthcheck 적용 (dmib 컨테이너 healthy 확인)
- A2: 미실행/실패 감지 스케줄러 + throttle(60분) 적용
- A3: runbook(RUNBOOK.md) + dmib.sh deploy/sync 표준화 적용

## 1) 목표 상태(To-Be)
- 운영 서버는 git pull로 배포하지 않는다.
- GitHub Actions가 이미지를 빌드/테스트/푸시하고,
- OCI 서버는 레지스트리에서 **이미지를 pull**만 한다.
- 문제가 생기면 runbook을 따라 5분 내 원인 범위를 좁힐 수 있다.

---

## 2) A4: CI/CD (GitHub Actions + OCIR + 서버 pull-only)

### A4-0 설계 결정
- Registry: OCIR(Oracle Container Registry)
- CI: GitHub Actions
- Deploy 방식: SSH로 OCI 접속해 `docker compose pull && up -d` 실행
- 이미지 태깅:
    - 안정 태그: `main`
    - 불변 태그: `sha-<GITHUB_SHA>`
    - 서버는 `sha-<GITHUB_SHA>`로 pin(권장)

### A4-1 OCIR 준비(수동 1회)
- [ ] OCIR Auth Token 생성
- [ ] Tenancy namespace 확인(Object Storage namespace)
- [ ] region-key 확인(예: icn, iad 등)
- [ ] OCIR repo 생성(dmib)
- [ ] OCI 서버에서 docker login 수행(1회)

Acceptance:
- `docker login <region-key>.ocir.io` 성공
- `docker pull <registry>/<namespace>/dmib:main` 가능(일단은 빈 repo면 불가 → CI push 후 가능)

### A4-2 GitHub Secrets 준비
- [ ] OCI_REGION_KEY
- [ ] OCI_TENANCY_NAMESPACE
- [ ] OCI_USERNAME (federated면 domain 포함)
- [ ] OCI_AUTH_TOKEN
- [ ] OCI_SSH_HOST (public IP)
- [ ] OCI_SSH_USER (= ubuntu)
- [ ] OCI_SSH_PRIVATE_KEY (pem 내용)
- [ ] DMIB_BASE=/home/ubuntu/ai_project/apps/dmib

Acceptance:
- Actions에서 registry login step 통과(보안상 로그 마스킹 확인)

### A4-3 Workflow 추가(.github/workflows/release.yml)
- [ ] checkout
- [ ] setup qemu/buildx
- [ ] login OCIR
- [ ] build+push multi-arch(arm64 포함)
- [ ] SSH deploy: 서버 runtime/.env의 DMIB_IMAGE 갱신 → compose pull/up
- [ ] health 확인까지 출력

Acceptance:
- main push 시 자동으로 OCIR에 이미지가 올라감
- workflow가 OCI에서 `dmib health`가 UP일 때 성공 처리

### A4-4 Compose 전환(서버 pull-only)
- [ ] repo/docker-compose.yml에서 dmib 서비스: `build:` 제거하고 `image:` 기반으로 전환
- [ ] runtime/.env에 `DMIB_IMAGE=` 추가(서버에서만 관리)
- [ ] dmib.sh deploy: `pull + up`로 변경(빌드 제거)
- Blocked: OCI Console 2FA reset required
  - Blocked scope:
    - OCIR Auth Token
    - Registry login
    - Image push/pull deployment
  - Resume trigger:
    - OCI Console login restored

Acceptance:
- OCI 서버에서 `git pull` 없이도 배포 가능
- `dmib deploy`가 “이미지 갱신”을 수행

### A4-5 롤백 전략(최소)
- [ ] 최근 sha 태그 3개 정도 기록(가장 쉬움: GitHub release note 또는 간단 로그 파일)
- [ ] rollback은 runtime/.env의 DMIB_IMAGE를 이전 sha로 바꾸고 `dmib restart`

---

## 3) 운영 강화(차후)
- [ ] 알림 정책: 실패 유형별 severity 분리(critical/warn)
- [ ] 통합 모니터링 프로젝트(여러 컨테이너 last-run 수집)
- [ ] 비용/호출량 추적(LLM 호출 횟수/토큰)

---

## 4) 반영 순서(절대 순서)
1) A4-1(OCIR 준비)
2) A4-2(GitHub Secrets)
3) A4-3(Workflow 추가)
4) A4-4(Compose/배포 스크립트 전환)
5) A4-5(롤백)
