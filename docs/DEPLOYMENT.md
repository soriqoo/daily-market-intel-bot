# 배포 가이드

## 문서 목적

이 문서는 DMIB의 현재 배포 방식, 목표 배포 방식, 그리고 전환 계획을 정리한다.

DMIB는 이미 실제 OCI 인스턴스에서 Docker로 운영 중이지만, 현재 방식은 학습과 운영 감각 습득에 적합한 1단계 모델이다. 장기적으로는 CI에서 이미지를 만들고 서버는 pull만 수행하는 구조로 전환하는 것이 목표다.

## 현재 배포 모델

### 환경 구성
- OCI Ubuntu 인스턴스
- Docker Compose
- PostgreSQL
- Slack Webhook 및 외부 시세 API 연동

### 서버 디렉터리 구조
현재 운영 구조 예시는 아래와 같다.

```text
/home/ubuntu/ai_project/apps/dmib
  data/
  logs/
  repo/
  runtime/
```

각 디렉터리의 역할:
- `repo/`: GitHub에서 받은 소스와 공개 가능한 문서
- `runtime/`: `.env`, 운영 스크립트, 배포 보조 파일
- `data/`: 영속 데이터
- `logs/`: 로그 파일

### 현재 배포 흐름
1. 서버의 `repo/`에서 소스를 최신화한다.
2. `runtime/`의 환경 변수와 스크립트를 사용한다.
3. Docker Compose로 재기동한다.
4. health와 Slack 결과를 확인한다.

## 현재 방식의 장점과 한계

### 장점
- 구조가 단순해서 빠르게 운영을 시작할 수 있다.
- 코드, 설정, 런타임의 경계를 이해하기 좋다.
- 배포 스크립트와 운영 명령을 체감하기 좋다.
- 개인 프로젝트이지만 서비스 운영 감각을 익히기에 충분하다.

### 한계
- 서버가 빌드 환경 역할까지 수행한다.
- `git pull` 기반 배포는 이미지 기반 배포보다 재현성이 약하다.
- 롤백 시 특정 이미지 태그 대신 Git 상태에 의존하기 쉽다.
- 서비스 수가 늘어나면 프로젝트별 배포 편차가 커질 수 있다.

## 목표 배포 모델

장기 목표는 아래와 같다.
- GitHub Actions에서 테스트와 이미지 빌드 수행
- Registry 또는 OCIR에 이미지 push
- 서버는 `docker compose pull`과 `docker compose up -d`만 수행

즉, 빌드는 CI가 담당하고 서버는 실행만 담당하는 구조로 전환한다.

## 현재 전환이 보류된 이유

현재는 OCI 로그인 관련 2FA 복구 이슈 때문에 OCIR 기반 전환이 잠시 보류된 상태다.
따라서 지금은 아래 방향이 현실적이다.
- 배포 구조와 문서를 먼저 정리한다.
- 서비스 공통 계약과 운영 패턴을 먼저 고도화한다.
- OCI 접근이 복구되면 image pull 기반 배포로 전환한다.

## 전환 목표 체크리스트

- [ ] OCIR auth token 발급
- [ ] OCI namespace 및 region 재확인
- [ ] GitHub Secrets 등록
- [ ] image build/push workflow 작성
- [ ] 서버 compose를 image 기반으로 전환
- [ ] 롤백 절차 문서화
- [ ] 통합 모니터링 프로젝트에도 동일 규칙 적용

## 권장 릴리스 흐름

### 현재 단계
1. `main`에 PR merge
2. CI 확인
3. 서버에서 코드 최신화
4. Docker Compose 재기동
5. health 및 Slack 결과 확인

### 목표 단계
1. `main`에 PR merge
2. CI에서 이미지 빌드 및 Registry push
3. 서버에서 이미지 pull
4. `docker compose up -d`
5. health 확인
6. 모니터링 서비스에서 상태 확인

## 배포 전 체크리스트

- 테스트가 통과했는가
- 환경 변수 변경이 필요한가
- DB 스키마 영향이 있는가
- Slack 메시지나 모니터링 정책이 바뀌는가
- 문서와 설정이 코드와 맞는가

## 장애 및 롤백 관점

현재 git pull 기반 모델에서는 롤백이 상대적으로 약하다. 따라서 지금 단계에서는 아래를 권장한다.
- 변경 단위를 작게 유지한다.
- feature 브랜치 단위로 PR을 나눈다.
- 배포 전에 테스트와 diff를 충분히 확인한다.
- 운영 스크립트와 로그 확인 절차를 문서화한다.

image pull 기반으로 전환되면 다음이 더 쉬워진다.
- 이미지 태그 기준 롤백
- 배포 결과 재현성 확보
- 여러 서비스 공통 배포 규칙 적용

## 향후 확장 원칙

다음에 만들 통합 모니터링 프로젝트나 다른 봇 서비스도 아래 기준을 따른다.
- `repo/`, `runtime/`, `data/`, `logs/` 분리
- `.env`와 운영 스크립트는 Git 비추적
- 배포는 점차 image pull 기반으로 통일
- health와 monitoring endpoint를 공통 제공
