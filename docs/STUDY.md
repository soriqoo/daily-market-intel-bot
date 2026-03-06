# STUDY.md — DMIB 운영/CI 학습 요약(초급 운영자용)

## 0) 이 프로젝트로 얻어야 하는 “운영 감각”
운영은 “문제 없게 만들기”가 아니라
**문제가 생겨도 빨리 발견하고(Detect), 영향 줄이고(Mitigate), 복구하고(Recover), 재발 방지(Prevent)** 하는 능력이다.

DMIB에서 얻는 운영 경험치:
- healthcheck로 “살아있음”을 기계적으로 판정
- job_run으로 “오늘 제대로 실행”을 검증
- 알림 throttle로 채널을 살림(알림 피로 방지)
- 배포 표준화로 실수 제거
- CI/CD로 서버를 빌드 환경에서 제외(불변 아티팩트)

---

## 1) 핵심 개념 치트시트
### Liveness vs Readiness vs Correctness
- Liveness: 프로세스 살아있나? (죽으면 재시작이 해결)
- Readiness: 의존성까지 준비됐나? (DB 다운은 재시작으로 해결 안 될 수 있음)
- Correctness: 오늘 8시에 리포트 보냈나?

추천 운영:
- Docker healthcheck는 liveness 위주
- Correctness는 별도 모니터링(미실행 감지 + Slack 알림)

### Throttle
- 같은 장애를 1분마다 보내면 Slack이 망가짐
- “60분에 1회”처럼 중복 억제 정책이 필요

### Immutable Artifact
- CI에서 빌드한 결과물을 레지스트리에 올리고
- 서버는 pull만 하는 방식
- 배포 재현성이 올라가고 “서버에서만 터지는 빌드 문제”가 사라짐

---

## 2) OCI 운영 명령 치트시트
### 배포/재기동
- `dmib deploy`
- `dmib restart`
- `dmib ps`
- `dmib logs`
- `dmib health`

### 모니터링/DB
- `/internal/monitoring/last-run` (내부 접근)
- `dmib db "select run_date,status,sent_at from job_run order by run_date desc limit 10;"`

---

## 3) Incident Log 템플릿(운영 경험치 쌓는 핵심)
runbook에 아래 형식으로 “사건이 생길 때마다” 5줄만 추가하면 된다.

### Incident YYYY-MM-DD
- 증상:
- 영향(사용자/채널):
- 원인:
- 조치:
- 재발 방지(코드/설정/운영):

---

## 4) “좋은 질문(프롬프트)” 템플릿 (너와 협업 효율 ↑)
문제 해결이 빨라지는 질문 포맷:

1) 목표(원하는 최종상태):
2) 현재 상태(어느 단계까지 됐는지):
3) 기대 동작 vs 실제 동작:
4) 로그/에러(가능한 원문):
5) 최근 변경(무엇을 바꿨는지):
6) 환경:
    - 로컬(Windows/IntelliJ)
    - 서버(OCI Ubuntu, docker compose 버전)
7) 제약(공개 repo, 비밀 노출 금지, 비용 제한 등)

---

## 5) 다음 학습 순서(지금 수준에 최적)
1) Actuator health의 의미(liveness/readiness)
2) compose 운영(override, env-file, ports 바인딩)
3) 레지스트리 인증(OCIR auth token)
4) GitHub Actions build/push(deploy)
5) 롤백/릴리즈 전략(sha tag, digest pin)

---

## 6) 추천 참고자료(핵심만)
- Spring Boot Actuator endpoints:
  https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- Spring liveness/readiness:
  https://spring.io/blog/2020/03/25/liveness-and-readiness-probes-with-spring-boot
- Docker Actions (build/push/login):
  https://docs.docker.com/build/ci/github-actions/
  https://github.com/docker/build-push-action
  https://github.com/docker/login-action
- Oracle OCIR push/pull:
  https://docs.oracle.com/en-us/iaas/Content/Registry/Tasks/registrypushingimagesusingthedockercli.htm
  https://docs.oracle.com/en-us/iaas/Content/Registry/Tasks/registrypullingimagesusingthedockercli.htm

---

## 7) CI를 어떻게 읽는가? (실무 요약)
GitHub Actions에서 초록 체크는 “그 커밋 기준으로 workflow가 성공”했다는 뜻이다.

### 기본 읽는 순서
1. 어떤 Job이 실패했는가?
2. 어느 Step에서 실패했는가?
3. 첫 번째 에러 메시지는 무엇인가?

### DMIB에서의 해석
- `Build (test + bootJar)` 실패
  -> 코드/테스트/Gradle 설정 문제
- `Docker Build Validation (arm64, thin)` 실패
  -> Dockerfile/packaging/build context 문제

### 중요한 개념
- CI는 운영 서버 배포와 별개다.
- `.github/workflows/*.yml` 변경은 GitHub에서만 실행된다.
- 앱 런타임 변경이 없는 경우 OCI 재배포는 필요 없다.

---

## 8) Matrix는 언제 쓰는가?
Matrix는 하나의 job 정의로 여러 조합(OS, Java 버전 등)을 자동으로 실행하는 기능이다.
DMIB 현재 단계에서는:
- 실제 런타임이 OCI Linux arm64
- ubuntu test + arm64 docker build로 핵심 검증이 충분

따라서 지금은 matrix를 도입하지 않는다.
복잡도/시간 대비 이득이 적기 때문이다.

---

## 9) 실무 브랜치 전략(혼자 개발 시)
현재 추천 전략:
- feature branch -> PR -> CI 통과 -> merge to main

dev 브랜치는 아직 도입하지 않는다.
이유:
- 단일 서비스/단일 운영 환경에서 규칙이 과도해질 수 있음
- main을 “배포 가능한 상태”로 유지하는 감각을 배우는 것이 우선

---

## 10) Protected main branch 이후 개발 흐름
main branch protection을 켜면 main에 직접 push할 수 없다.
이후 표준 개발 흐름은 다음과 같다.

1. main 최신화
  - `git switch main`
  - `git pull --ff-only`

2. feature branch 생성
  - `git switch -c feature/<task-name>`

3. 작업 후 push
  - `git add .`
  - `git commit -m "..."`
  - `git push -u origin feature/<task-name>`

4. PR 생성
  - GitHub에서 feature -> main PR 생성
  - CI 통과 확인
  - merge

5. 로컬 정리
  - `git switch main`
  - `git pull --ff-only`
  - `git branch -d feature/<task-name>`

핵심:
- main은 배포 가능한 상태를 유지한다.
- 실험/변경은 feature branch에서만 한다.
