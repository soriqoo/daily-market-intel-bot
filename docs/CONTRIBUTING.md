# CONTRIBUTING

## 문서 목적

이 저장소는 개인 프로젝트이지만, 실무와 유사한 브랜치/PR/CI 흐름으로 운영한다.
이 문서는 작업 단위, 리뷰 기준, merge 후 정리 규칙을 간단히 정리한 문서다.

## 기본 브랜치 전략

- `main`
  - 항상 배포 가능한 상태를 유지한다.
  - 직접 push하지 않는다.
- `feature/<task-name>`
  - 모든 작업은 feature 브랜치에서 시작한다.
  - 작업 완료 후 PR로 `main`에 merge한다.

## 표준 작업 흐름

```bash
git switch main
git pull --ff-only
git switch -c feature/<task-name>

# work...
git add .
git commit -m "..."
git push -u origin feature/<task-name>
```

## PR 기준

PR에는 아래가 포함되는 것이 좋다.
- 변경 목적
- 주요 변경 파일
- 테스트 또는 검증 결과
- 운영 영향이 있는지 여부
- 남아 있는 리스크 또는 후속 작업

## 리뷰 기준

리뷰 시 우선적으로 보는 항목:
- 버그 가능성
- 회귀 위험
- 운영 영향
- 테스트 누락
- 문서와 설정 반영 여부

## Merge 정책

- GitHub에서 PR 생성
- CI 통과 확인
- merge 후 로컬 `main` 최신화
- merge가 끝난 feature 브랜치만 삭제

merge 후 권장 흐름:

```bash
git switch main
git pull --ff-only
git branch -d feature/<task-name>
```

## 문서와 설정 반영 규칙

아래 변경은 가능하면 코드와 함께 반영한다.
- 운영 방식 변경
- 배포 흐름 변경
- 내부 API 정책 변경
- 모니터링 정책 변경
- 새 환경 변수 추가

## 커밋 단위 원칙

- 하나의 커밋은 하나의 주제를 담는 것이 좋다.
- 기능 추가와 문서 정리는 가능하면 같은 맥락일 때만 묶는다.
- 생성 파일은 의도하지 않았다면 커밋하지 않는다.
