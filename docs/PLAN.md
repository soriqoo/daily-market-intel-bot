# 실행 계획

## 프로젝트 목표

DMIB를 단순 자동화 스크립트가 아니라 운영 가능한 백엔드 서비스로 발전시킨다.

핵심 목표:
- 매일 안정적으로 시장 브리프를 전송한다.
- 실패와 미실행을 감지하고 리마인드한다.
- 코드, 테스트, 운영 문서를 함께 관리한다.
- 이후 통합 모니터링 서비스와 여러 봇으로 확장 가능한 템플릿을 만든다.

## 현재까지 완료한 항목

- Actuator와 Docker healthcheck 적용
- 실행 결과 저장(`job_run`)
- Slack 전송 및 payload hash 기반 중복 방지
- 부분 실패 허용 정책 적용
- 모니터링 API(`/internal/monitoring/last-run`) 추가
- 미실행/실패 감지 스케줄러 추가
- 첫 실패 즉시 알림 + 정각 리마인드 정책 적용
- 내부 테스트 엔드포인트 기본 비활성화
- 핵심 통합 테스트 추가
- README 및 운영 문서 정리
- GitHub Actions CI 구축
  - Gradle test
  - bootJar
  - Docker build validation
- feature branch -> PR -> merge 워크플로우 적용

## 현재 보류 중인 항목

- OCI Console 2FA 복구 전까지 OCIR 기반 배포 전환 보류

## 다음 우선순위

1. 공통 monitoring contract를 더 명확하게 문서화
2. DMIB를 통합 모니터링 대상 서비스의 첫 구현체로 정리
3. 통합 모니터링 서비스 별도 repo 설계 시작
4. Testcontainers나 Redis가 필요한 실제 요구 정의
5. OCI/OCIR 기반 image pull 배포 전환 재개

## 배포 전환 계획

### 목표 상태
- GitHub Actions에서 이미지 빌드와 검증 수행
- Registry push
- 서버는 `pull + up -d`만 수행

### 체크리스트
- [ ] OCIR auth token 발급
- [ ] namespace / region 확인
- [ ] GitHub Secrets 등록
- [ ] image build/push workflow 추가
- [ ] compose의 image 기반 전환
- [ ] rollback 절차 문서화

## 템플릿 관점의 현재 수준

현재 DMIB는 아래 역할을 할 수 있다.
- 실제 운영 중인 Slack 자동화 서비스
- 운영형 백엔드 연습용 프로젝트
- 향후 다른 봇 프로젝트의 기본 템플릿
- 통합 모니터링 서비스의 첫 monitored service

## 현재 상태 점검표

- [x] Health endpoint
- [x] Docker healthcheck
- [x] Monitoring endpoint
- [x] Execution record
- [x] Slack alerting
- [x] Idempotency
- [x] Partial failure policy
- [x] Monitoring scheduler
- [x] Monitoring reminder policy
- [x] Internal test endpoint protection
- [x] Standard deploy script
- [x] CI validation
- [ ] Registry-based deployment
- [ ] Integrated monitoring service
- [ ] Infra-backed tests with Testcontainers
