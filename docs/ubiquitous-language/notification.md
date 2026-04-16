# Notification

> 사용자 알림 토큰 저장과 활성화 상태를 다루는 지원 바운디드 컨텍스트입니다.

## Owning packages

- `src/main/java/com/techfork/domain/notification`

## 표준 용어

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 알림 토큰 | `NotificationToken` | 사용자에게 푸시/알림을 보낼 수 있는 토큰 |
| 활성 토큰 | `isActive` | 현재 발송 가능한 알림 토큰 상태 |

## 경계 메모

- 현재는 얇은 컨텍스트이므로 용어 수가 적다.
- 추천 알림/푸시 기능이 확장되면 별도 이벤트/전송 용어를 추가한다.

## 내부 glossary

| 내부 용어 | 코드상 표현 | 설명 |
|---|---|---|
| 사용자별 토큰 | `NotificationToken.user` | 어떤 사용자에게 귀속된 토큰인지 나타내는 참조 |
| 활성 상태 | `isActive` | 발송 가능 여부를 나타내는 상태 |

## 혼동 금지

- `NotificationToken`은 메시지 발송 이력이나 알림 콘텐츠를 뜻하지 않는다.
- 현재 Notification 컨텍스트는 **토큰 저장**에 가깝고, 실제 푸시 발송 도메인은 아직 충분히 모델링되지 않았다.

## 금지 표현 / 권장 표현

| 금지/비권장 표현 | 권장 표현 | 이유 |
|---|---|---|
| 알림 | 알림 토큰 / 알림 발송 | 토큰 저장과 실제 발송을 구분해야 한다 |
| 푸시 | 알림 토큰 / 푸시 알림 | 저장 모델과 채널 동작을 섞지 않기 위해 |
| 사용자 알림 정보 | 사용자별 알림 토큰 | 현재 모델의 실체가 토큰 저장이라는 점을 드러낸다 |

## 주요 근거 파일

- `src/main/java/com/techfork/domain/notification/entity/NotificationToken.java`
