# PLAN.md

## Goal

DMIB를 단순 자동화 스크립트가 아니라 운영 가능한 백엔드 서비스로 발전시킨다.

핵심 목표:
- 매일 안정적으로 시장 브리프를 전송한다.
- 실패와 미실행을 감지한다.
- 운영 절차와 문서를 함께 관리한다.
- 이후 통합 모니터링 서비스와 여러 봇으로 확장 가능한 구조를 만든다.

## Completed

- Actuator + Docker healthcheck 적용
- 실행 결과 저장(`job_run`)
- Slack 전송 + payload hash 기반 중복 방지
- 부분 실패 허용 정책 적용
- 모니터링 API(`/internal/monitoring/last-run`) 추가
- 미실행/실패 감지 스케줄러 추가
- 알림 리마인드 정책 개선
- Runbook / 배포 표준화 문서 작성
- GitHub Actions CI 구축
  - Gradle test
  - bootJar
  - Docker build validation
- feature branch -> PR -> merge 워크플로우 적용

## In Progress

- 운영 문서 UTF-8 정리
- 메시지/주석 인코딩 정리
- 모니터링 정책 리팩터링 후 운영 검증

## Blocked

- OCI Console 2FA 복구 전까지 OCIR 기반 배포 전환 보류

## Next

1. 공통 모니터링 API 계약 정리
2. DMIB를 통합 모니터링 대상 서비스의 첫 구현체로 정리
3. 내부 API 노출/보안 정책 점검
4. 문서 공통 템플릿 정리
5. OCIR 기반 배포 전환 재개

## Deployment Transition Plan

### Target
- GitHub Actions에서 이미지 빌드/검증
- Registry push
- 서버는 `pull + up -d`만 수행

### Checklist
- [ ] OCIR auth token 발급
- [ ] namespace / region 확인
- [ ] GitHub Secrets 등록
- [ ] release workflow 추가
- [ ] compose를 image 기반으로 전환
- [ ] rollback 절차 문서화

## Current Compliance

- [x] Health endpoint
- [x] Docker healthcheck
- [x] Monitoring endpoint
- [x] Execution record
- [x] Slack alerting
- [x] Idempotency
- [x] Partial failure policy
- [x] Monitoring scheduler
- [x] Monitoring reminder policy
- [x] Standard deploy script
- [x] CI validation
- [ ] Registry-based deployment
- [ ] Integrated monitoring service
