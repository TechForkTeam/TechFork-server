# TechFork 유비쿼터스 언어 사전

> 이 문서는 **호환용 인덱스**입니다. 상세 표준 용어는 `docs/ubiquitous-language/` 디렉터리 문서를 사용합니다.  
> 관련 문서: [도메인 전략 설계](domain-strategy.md) | [전술 설계](tactical-design.md) | [유비쿼터스 언어 README](ubiquitous-language/README.md)

---

## 문서 사용 순서

1. 컨텍스트 경계와 관계는 [`docs/domain-strategy.md`](./domain-strategy.md)에서 확인한다.
2. 애그리거트/이벤트/불변식은 [`docs/tactical-design.md`](./tactical-design.md)에서 확인한다.
3. 표준 용어는 아래 컨텍스트별 glossary 문서에서 확인한다.

## 컨텍스트별 glossary

- [README / 사용 원칙](./ubiquitous-language/README.md)
- [Source / Ingestion](./ubiquitous-language/source-ingestion.md)
- [Post / Content](./ubiquitous-language/post-content.md)
- [User Account](./ubiquitous-language/user-account.md)
- [Personalization Profile](./ubiquitous-language/personalization-profile.md)
- [User / Profile (legacy bridge)](./ubiquitous-language/user-profile.md)
- [Activity](./ubiquitous-language/activity.md)
- [Search](./ubiquitous-language/search.md)
- [Recommendation](./ubiquitous-language/recommendation.md)
- [Auth / Security](./ubiquitous-language/auth-security.md)
- [Notification](./ubiquitous-language/notification.md)
- [Admin / Ops](./ubiquitous-language/admin-ops.md)

## 핵심 정리

- 유비쿼터스 언어 문서는 **바운디드 컨텍스트 기준**으로 나눈다.
- 패키지는 참고 경로일 뿐, 문서의 1차 분리 기준이 아니다.
- 전략 문서와 glossary에서는 `User Account`와 `Personalization Profile`을 개념적으로 분리한다.
- 다만 현재 구현 패키지는 여전히 `domain/user` 아래에 함께 있다.
- `Auth`는 문서 표준에서 **`Auth / Security`** 로 표기해 `global/security`까지 포함하는 실제 경계를 반영한다.
