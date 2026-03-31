# 운영 Runbook

## 문서 목적

이 문서는 DMIB 운영자가 평소 확인해야 할 항목과 장애 시 기본 대응 순서를 정리한 문서다.

원칙은 단순하다.
- 먼저 상태를 확인한다.
- 바로 수정하지 말고 증상과 로그를 확인한다.
- 수동 조치 전에 원인과 영향 범위를 파악한다.

## 현재 운영 환경

- OCI Ubuntu
- Docker Compose
- PostgreSQL
- Slack Webhook 연동
- 외부 데이터 소스: FRED, 환율 API

운영 경로 예시:

```text
/home/ubuntu/ai_project/apps/dmib
  data/
  logs/
  repo/
  runtime/
```

`runtime/.env`에는 운영 비밀이 들어가며 Git에 올리지 않는다.

## 표준 운영 명령

> `dmib`는 `runtime/dmib.sh`를 가리키는 alias 또는 wrapper 스크립트라고 가정한다.

주요 명령:
- `dmib deploy`
- `dmib restart`
- `dmib ps`
- `dmib logs`
- `dmib health`

## 일상 점검 항목

매일 또는 배포 후 아래를 확인한다.
- 컨테이너가 살아 있는가
- `/actuator/health`가 정상인가
- 오늘 `job_run`이 기록되었는가
- Slack에 브리프가 한 번만 전송되었는가
- 미실행/실패 알림이 unresolved 상태로 남아 있지 않은가

## 내부 점검용 엔드포인트

현재 내부 테스트 엔드포인트는 기본적으로 비활성화되어 있다.
필요할 때만 설정으로 열어야 한다.

대상:
- `POST /internal/test/run`
- `POST /internal/test/slack`

운영 원칙:
- 기본 비활성
- 점검이 끝나면 다시 비활성
- 외부 공개 환경에서는 상시 노출 금지

## 장애 발생 시 1차 대응 순서

### 1. health 확인
- 컨테이너가 살아 있는가
- DB 연결이 정상인가

### 2. 최근 실행 상태 확인
- 오늘 `job_run`이 존재하는가
- 상태가 `SENT`인지 `FAILED`인지 확인한다.
- `sentAt`, `error` 값을 확인한다.

### 3. 로그 확인
- 애플리케이션 로그
- Docker Compose 로그
- 외부 API 호출 실패 흔적
- Slack 전송 실패 흔적

### 4. 원인 분류
- 외부 API 실패
- Slack Webhook 실패
- DB 기록 실패
- 스케줄 미실행
- 배포 직후 설정 오류

### 5. 수동 조치 여부 판단
- 서비스 재시작이 필요한가
- 일시 장애인지 확인이 필요한가
- 수동 재실행이 필요한가

## 배포 절차

현재 모델 기준 권장 절차:
1. `main`에 merge된 변경인지 확인
2. CI 결과 확인
3. 서버에서 최신 코드 반영
4. Docker Compose 재기동
5. health 확인
6. Slack 브리프 또는 monitoring 상태 확인

## 배포 후 확인 항목

- `dmib ps`
- `dmib health`
- 애플리케이션 로그 이상 여부
- `job_run` 상태 정상 기록 여부
- 모니터링 알림이 과도하게 발생하지 않는지

## 수동 재실행 시 주의사항

- 중복 전송 방지 로직이 있지만, 운영 중 수동 실행은 항상 주의해서 수행한다.
- Slack 전송이 이미 성공한 상태에서 반복 재실행하면 운영자가 혼란스러울 수 있다.
- 수동 실행 전에는 오늘 `job_run` 상태와 Slack 전송 여부를 먼저 확인한다.

## 운영 메모 정책

개인 메모, 호스트 정보, 운영 중에만 필요한 값은 `RUNBOOK.private.md`에 따로 관리한다.
이 파일은 예시만 저장소에 두고, 실제 값은 개인 로컬 또는 서버에서만 관리한다.
