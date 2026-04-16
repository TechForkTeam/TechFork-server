# Admin / Ops

> 운영자 수동 실행, 배치 트리거, 운영 알림 같은 운영 유스케이스를 다루는 지원 컨텍스트입니다.

## Owning packages

- `src/main/java/com/techfork/domain/admin`
- 연관 운영 서비스: `src/main/java/com/techfork/domain/source`, `src/main/java/com/techfork/domain/post/batch`

## 표준 용어

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 관리자 배치 실행 | `AdminController` | 요약/임베딩 배치, RSS 크롤링을 수동 실행하는 운영 기능 |
| 수동 크롤링 실행 | `crawlRss` | 모든 테크 블로그 RSS를 수동으로 다시 수집하는 행위 |
| 운영 알림 | `WebhookNotificationService` | 크롤링 실패 같은 운영 이벤트를 외부 채널로 전송하는 기능 |
| 개발자 토큰 발급 | `generateDeveloperToken` | 관리자용 장기 액세스 토큰을 발급하는 운영 기능 |

## 경계 메모

- `Admin / Ops`는 강한 도메인 모델보다 **운영 진입점(application context)** 성격이 더 강하다.
- `Notification`과 연계는 있지만, 현재는 알림 토큰 도메인과 운영 행동을 분리해서 문서화한다.

## 내부 glossary

| 내부 용어 | 코드상 표현 | 설명 |
|---|---|---|
| 관리자 진입점 | `AdminController` | 운영자가 직접 호출하는 API entrypoint |
| 수동 배치 실행 | `summaryAndEmbeddingJob`, `crawlRss` | 운영자가 강제로 파이프라인을 돌리는 실행 경로 |
| 운영 채널 알림 | `WebhookNotificationService` | 실패나 운영 이벤트를 외부 채널에 보내는 기능 |
| 개발 지원 토큰 | `generateDeveloperToken` | 프론트 개발/운영 테스트용 장기 액세스 토큰 발급 기능 |

## 혼동 금지

- `Admin / Ops`는 핵심 비즈니스 모델이라기보다 운영 유스케이스 모음에 가깝다.
- `개발자 토큰`은 Auth / Security의 토큰 메커니즘을 사용하지만, 운영 진입점은 Admin / Ops가 제공한다.

## 금지 표현 / 권장 표현

| 금지/비권장 표현 | 권장 표현 | 이유 |
|---|---|---|
| 어드민 도메인 | Admin / Ops 운영 컨텍스트 | 핵심 비즈니스 모델보다 운영 유스케이스 성격이 강하다 |
| 배치 돌리기 | 수동 배치 실행 | 운영 행위라는 점을 명확히 한다 |
| 관리자 토큰 | 개발자 토큰 | 현재 API 명칭과 맞춘다 |
| 알림 | 운영 알림 | Notification 토큰 도메인과 구분해야 한다 |

## 주요 근거 파일

- `src/main/java/com/techfork/domain/admin/controller/AdminController.java`
- `src/main/java/com/techfork/domain/source/service/CrawlingService.java`
