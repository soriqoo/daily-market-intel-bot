# DEPLOYMENT.md

## 1. 목적

이 문서는 DMIB의 현재 배포 방식과 향후 목표 배포 방식을 정리한다.

이 문서의 목적은 다음과 같다.

1. 현재 운영 절차를 표준화한다.
2. “코드 / 운영 설정 / 비밀”의 경계를 명확히 한다.
3. 향후 GitHub Actions + Registry 기반 배포로 전환할 때 혼선이 없도록 한다.
4. 운영자가 “지금은 무엇을 어떻게 배포하고 있고, 이후 무엇으로 전환할 예정인지”를 한 번에 이해하게 한다.

---

## 2. 현재 배포 모델 (As-Is)

현재 DMIB는 다음 방식으로 배포된다.

### 배포 환경
- OCI Ubuntu 인스턴스
- Docker Compose 기반
- 애플리케이션과 PostgreSQL 컨테이너 함께 운영
- Slack/Gemini/FX/FRED 연동 포함

### 현재 특징
- 서버에서 compose 실행
- runtime/.env에서 비밀값 주입
- repo/docker-compose.override.yml로 운영 환경 차이 반영
- `dmib.sh` 스크립트로 표준화된 배포/재기동/상태 확인 수행

### 현재 장점
- 구조가 단순하다
- 학습/개인 운영에 적합하다
- 문제를 직접 관찰하고 디버깅하기 쉽다

### 현재 한계
- 서버가 빌드 환경 역할을 한다
- `git pull` 기반 배포는 장기적으로 불변성이 낮다
- 이미지 롤백/버전 추적이 레지스트리 기반 배포보다 약하다

---

## 3. 목표 배포 모델 (To-Be)

향후 목표는 다음과 같다.

### 목표
- GitHub Actions가 빌드/테스트/이미지 푸시 수행
- OCI 서버는 이미지를 **pull만** 함
- 서버는 더 이상 “소스 빌드 환경”이 아니다

### 기대 효과
- 배포 재현성 증가
- 서버에서 발생하는 빌드/권한/캐시 문제 감소
- 버전 추적/롤백이 쉬워짐
- 운영과 개발 책임 경계가 명확해짐

---

## 4. 디렉터리/파일 구조 원칙

### OCI 서버 표준 경로
- Base Path: `/home/ubuntu/ai_project/apps/dmib`

### 역할 분리
#### repo/
- GitHub 저장소 clone 위치
- 공개 가능한 코드/문서/템플릿만 포함
- 운영 서버에서 직접 수정 금지

#### runtime/
- 실 운영 비밀과 운영 스크립트 보관
- 예:
  - `.env`
  - `dmib.sh`
- Git 추적 대상 아님

### 현재 예시
```text
/home/ubuntu/ai_project/apps/dmib
  ├── repo/
  │   ├── docker-compose.yml
  │   ├── docker-compose.override.yml   # 서버 전용, 커밋 금지
  │   └── (code)
  └── runtime/
      ├── .env
      └── dmib.sh


