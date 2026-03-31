# STUDY.md

## Purpose

이 문서는 DMIB를 통해 서비스 기업 백엔드 개발자가 익히기 좋은 운영 개념과 실무 습관을 정리한다.

## 핵심 개념

### Liveness vs Correctness
- Liveness: 프로세스와 컨테이너가 살아 있는가
- Correctness: 오늘 해야 할 실행이 실제로 성공했는가

### Alert fatigue
- 같은 장애를 너무 자주 보내면 운영자가 무시하게 된다
- 그래서 체크 주기와 알림 주기를 분리해야 한다

### Immutable artifact
- CI에서 빌드
- 서버는 pull과 실행만 담당
- 서버 빌드 의존성을 줄인다

## 운영 체크리스트

- Health endpoint가 있는가
- 실행 결과가 DB에 기록되는가
- 미실행/실패 감지가 되는가
- 중복 알림이 억제되는가
- 배포 절차가 문서화되어 있는가
- 장애 대응 절차가 있는가

## OCI 운영 치트시트

- `dmib deploy`
- `dmib restart`
- `dmib ps`
- `dmib logs`
- `dmib health`

## 장애 메모 템플릿

### Incident YYYY-MM-DD
- 증상:
- 영향:
- 원인:
- 조치:
- 재발 방지:

## 질문 템플릿

1. 목표가 무엇인가
2. 현재 상태는 어떤가
3. 기대 동작과 실제 동작이 어떻게 다른가
4. 로그와 에러는 무엇인가
5. 최근 무엇을 바꿨는가
6. 로컬/서버 환경은 무엇인가

## 다음 학습 순서

1. Actuator health와 probe
2. Docker Compose 운영
3. Registry 인증
4. GitHub Actions build/push/deploy
5. 롤백 전략
