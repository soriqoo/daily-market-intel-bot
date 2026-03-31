# DEPLOYMENT.md

## Purpose

이 문서는 DMIB의 현재 배포 방식과 목표 배포 방식을 정리한다.

## Current model

### Environment
- OCI Ubuntu
- Docker Compose
- PostgreSQL
- Slack / external API integration

### Current process
- 서버에서 compose 실행
- `runtime/.env`로 운영 비밀 주입
- 필요 시 override 파일 사용
- `dmib.sh`로 배포와 상태 확인

### Pros
- 구조가 단순하다
- 개인 프로젝트에 빠르게 적용 가능하다
- 운영 감각을 익히기 좋다

### Cons
- 서버가 빌드 환경 역할도 겸한다
- `git pull` 기반 배포는 재현성이 떨어질 수 있다
- 이미지 버전 추적과 롤백이 상대적으로 약하다

## Target model

- GitHub Actions에서 빌드와 검증 수행
- Registry에 이미지 push
- 서버는 `pull`과 `up -d`만 수행

## Directory policy

### repo/
- 코드와 공개 가능한 문서
- Git으로 관리

### runtime/
- `.env`
- 운영 스크립트
- Git 비추적

## Recommended flow

1. CI에서 이미지 빌드
2. Registry push
3. 서버에서 로그인
4. `docker compose pull`
5. `docker compose up -d`
6. health 확인
