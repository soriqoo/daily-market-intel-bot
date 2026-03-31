# CONTRIBUTING.md

## Purpose

이 저장소는 개인 프로젝트이지만, 실무와 유사한 브랜치/PR/CI 흐름으로 운영한다.

## Branch strategy

- `main`
  - 항상 배포 가능한 상태를 유지한다
  - 직접 push하지 않는다
- `feature/<task-name>`
  - 모든 작업은 feature branch에서 시작한다
  - 작업 완료 후 PR로 `main`에 merge한다

## Standard workflow

```bash
git switch main
git pull --ff-only
git switch -c feature/<task-name>

# work...
git add .
git commit -m "..."
git push -u origin feature/<task-name>
```

## Merge policy

- GitHub에서 PR 생성
- CI 통과 확인
- merge 후 로컬 main을 최신화
- merge가 끝난 feature branch만 삭제

## Review focus

- 버그와 회귀 위험
- 운영 영향
- 테스트 누락
- 문서/설정 반영 여부
