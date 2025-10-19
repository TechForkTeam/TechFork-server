# RSS 크롤링 스케줄러 가이드

## 개요

TechFork의 RSS 크롤링 스케줄러는 1시간마다 자동으로 RSS 피드를 수집하고, 실행 이력을 기록하며, 실패 시 알림을 전송합니다.

## 주요 기능

### 1. 자동 크롤링 (매 시간 정각)
- **스케줄**: 매 시간 정각 (00분)
- **동작**: RSS 피드를 자동으로 수집하여 데이터베이스에 저장
- **중복 방지**: Redis 분산 락을 사용하여 동시 실행 방지

### 2. 크롤링 실행 이력 관리
- 모든 크롤링 실행 내역을 `crawling_histories` 테이블에 저장
- 저장 정보:
  - 실행 상태 (RUNNING, SUCCESS, FAILED)
  - 시작/종료 시간
  - 처리 건수 (전체, 성공, 실패)
  - 에러 메시지 (실패 시)
  - Spring Batch Job Execution ID

### 3. Redis 분산 락
- 여러 서버 인스턴스가 동시에 크롤링을 실행하는 것을 방지
- 락 키: `lock:rss-crawling`
- 락 유지 시간: 30분 (크롤링 최대 실행 시간)

### 4. 실패 알림 (Discord)
- 크롤링 실패 시 자동으로 Webhook 알림 전송
- 알림 내용:
  - 에러 메시지
  - Job Execution ID
  - 발생 시간

### 5. 좀비 프로세스 정리
- **스케줄**: 5분마다
- **동작**: 1시간 이상 RUNNING 상태인 이력을 FAILED로 변경
- 서버 장애 등으로 인한 좀비 프로세스 자동 정리

## 설정 방법

### 1. application-local.yml 설정

```yaml
# Webhook 알림 활성화 (선택사항)
webhook:
  enabled: true  # false이면 알림 비활성화
  discord:
    url: https://discord.com/api/webhooks/YOUR/WEBHOOK/URL
```

### 2. Redis 설정 확인

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: redis1234
```

## 수동 실행 방법

스케줄러를 기다리지 않고 수동으로 크롤링을 실행하려면 기존의 BatchController를 사용하세요:

## 모니터링
### 애플리케이션 로그 확인

```bash
# 스케줄러 실행 로그
grep "RSS crawling scheduler triggered" application.log

# 크롤링 완료 로그
grep "RSS crawling completed successfully" application.log

# 크롤링 실패 로그
grep "RSS crawling failed" application.log
```

## 스케줄 변경 방법

스케줄을 변경하려면 `RssCrawlingScheduler.java`의 `@Scheduled` 어노테이션을 수정하세요:

```java
// 현재: 매 시간 정각
@Scheduled(cron = "0 0 * * * *")

// 예시: 30분마다
@Scheduled(cron = "0 */30 * * * *")

// 예시: 매일 오전 9시
@Scheduled(cron = "0 0 9 * * *")

// 예시: 평일 오전 9시~오후 6시, 매 시간
@Scheduled(cron = "0 0 9-18 * * MON-FRI")
```

### Cron 표현식 형식

```
초 분 시 일 월 요일
0  0  *  *  *  *

0-59: 초
0-59: 분
0-23: 시
1-31: 일
1-12: 월
0-7: 요일 (0, 7 = 일요일)
```

## 트러블슈팅

### 1. 스케줄러가 실행되지 않음
- `@EnableScheduling`이 `SchedulerConfig`에 있는지 확인
- 애플리케이션 로그에서 스케줄러 초기화 확인

### 2. 중복 실행 발생
- Redis 연결 상태 확인
- 분산 락 로그 확인: `"Lock acquired"` 메시지 확인

### 3. 알림이 전송되지 않음
- `webhook.enabled: true` 설정 확인
- Webhook URL이 올바른지 확인
- 네트워크 연결 상태 확인

### 4. 크롤링 이력이 저장되지 않음
- MySQL 연결 상태 확인
- `crawling_histories` 테이블이 생성되었는지 확인

## 운영 시 주의사항

### 1. 락 타임아웃
- 기본 락 유지 시간: 30분
- 크롤링이 30분 이상 걸리면 락이 자동으로 해제됨
- 필요시 `LOCK_TTL` 값 조정

### 2. 데이터베이스 용량
- 크롤링 이력이 계속 쌓이므로 주기적으로 정리 필요
- 권장: 6개월~1년 이상된 SUCCESS 이력 삭제

### 3. Webhook Rate Limit
- Slack/Discord는 초당 요청 수 제한이 있음
- 실패 알림이 너무 자주 발생하면 Rate Limit 발생 가능

### 4. 성능 최적화
- `rssTaskExecutor` 설정으로 병렬 처리 조절
- 현재 설정: Core 5개, Max 10개 스레드
- RSS 피드 개수가 많으면 스레드 수 증가 고려

## API 엔드포인트 (향후 추가 예정)

```
GET  /api/crawling/histories         - 크롤링 이력 목록 조회
GET  /api/crawling/histories/{id}    - 특정 이력 상세 조회
POST /api/crawling/trigger            - 수동 크롤링 트리거
GET  /api/crawling/status             - 현재 크롤링 상태 조회
```
