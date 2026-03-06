# CONTRIBUTING.md

## Purpose
이 저장소는 개인 프로젝트이지만, 실무와 유사한 브랜치/PR/CI 흐름을 훈련하기 위해 아래 규칙을 따른다.

## Branch Strategy
- `main`:
    - 항상 배포 가능 상태를 유지
    - 직접 push 금지
- `feature/<task-name>`:
    - 모든 작업은 feature branch에서 시작
    - 작업 완료 후 PR을 통해 `main`에 merge

## Standard Workflow
1. 최신 main 동기화
   ```bash
   git switch main
   git pull --ff-only
