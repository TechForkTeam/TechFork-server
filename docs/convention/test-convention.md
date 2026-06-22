# 테스트 컨벤션

## 테스트 실행 레인 계약

테스트는 Gradle task와 JUnit tag 조합으로 실행 레인을 나눈다.

| 레인 | 실행 명령 | 태그 계약 | 용도 |
| --- | --- | --- | --- |
| unit / slice | `./gradlew test -PexcludeIntegration` | `integration`, `evaluation`, `evaluation-setup` 제외 | 일반 단위 테스트와 JPA slice 등 빠른 회귀 확인 |
| default test | `./gradlew test` | `evaluation`, `evaluation-setup` 제외 | Gradle 기본 테스트. `integration` 태그는 제외하지 않으므로 빠른 로컬 루프에는 권장하지 않음 |
| integration | `./gradlew integrationTest` | `integration` 포함, `evaluation`, `evaluation-setup` 제외 | `MySqlRedisIntegrationTestBase` 기반 API/트랜잭션/컨테이너 통합 검증 |
| evaluation | `./gradlew evaluationTest` | `evaluation` 포함, `integration`, `evaluation-setup` 제외 | 검색/추천 품질 평가 |
| evaluation-setup | `./gradlew evaluationSetup` | `evaluation-setup` 포함 | 평가 fixture 생성/export 등 사전 데이터 준비 |

- 빠른 리팩터링 검증 루프는 `./gradlew test -PexcludeIntegration`를 기본으로 한다.
- `@SpringBootTest` 기반 일반 통합 테스트는 기본적으로 `MySqlRedisIntegrationTestBase`를 상속해 `integration` 레인에 둔다.
- 빈 Spring context smoke test는 일반 `test` 레인에 두지 않는다. 유지가 필요하면 `integration` 레인 계약을 명확히 적용한다.
- evaluation 하위 테스트는 일반 unit/integration 회귀 검증과 섞지 않는다.

## 패키지 컨벤션

- 테스트 패키지는 `com.techfork.<bounded-context>.<test-kind>`를 기본으로 한다.
  - 예: `post.domain`, `post.infrastructure`, `post.integration`, `useraccount.presentation.request`, `auth.integration`
- `test-kind` 패키지는 테스트의 역할을 드러내는 이름을 사용한다.
  - 예: `domain`, `application`, `infrastructure`, `presentation`, `integration`, `fixture`
- production 패키지가 아직 `com.techfork.domain.*`에 남아 있는 영역은 테스트도 legacy root를 유지한다.
- legacy slice를 옮길 때는 production과 test를 같은 작업 단위에서 함께 옮긴다. 네이밍 정리만을 위해 테스트만 먼저 이동하지 않는다.
- `src/test/java/com/techfork/evaluation`은 별도 evaluation lane이다. 해당 하위 패키지는 [로컬 가이드](../../src/test/java/com/techfork/evaluation/AGENTS.md)를 따른다.

## Legacy `domain/*` 유지/이관 기준

현재 production package가 `com.techfork.domain.*`에 남아 있는 영역은 테스트도 같은 legacy root를 유지한다.

- 현재 legacy 유지 대상:
  - `com.techfork.domain.source`
  - `com.techfork.domain.search`
  - `com.techfork.domain.recommendation`
- 테스트만 먼저 `com.techfork.<context>`로 옮기지 않는다. production과 test의 root가 달라지면 IDE 탐색, package-private 접근, fixture 소유권 판단이 어긋날 수 있다.
- legacy slice를 이관할 때는 다음을 같은 작업 단위로 묶는다.
  - production package 선언과 디렉터리 이동
  - test package 선언과 디렉터리 이동
  - fixture package와 import 경로 갱신
  - 문서/AGENTS/로드맵의 package 경로 갱신
- 단순 네이밍 정리나 fixture 정리 작업에서는 legacy root를 유지하고, 이관은 별도 리팩터링 이슈로 분리한다.

## 네이밍 컨벤션

### 클래스명

| 대상 | 기본 이름 | 예외/비고 |
| --- | --- | --- |
| 도메인 객체/값 객체 | `{Subject}Test` | 예: `PostTest`, `UserTest` |
| application/service 단위 테스트 | `{Subject}Test` | production class 이름을 따른다. 예: `PostQueryServiceTest` |
| Repository 검증 테스트 | `{RepositoryName}Test` | `DataJpaTest` suffix를 기본값으로 쓰지 않는다. 예: `PostRepositoryTest` |
| 같은 subject에 unit과 persistence slice가 공존하는 batch/reader/writer | `{Subject}DataJpaTest` 허용 | 예: `PostSummaryReaderTest` + `PostSummaryReaderDataJpaTest` |
| HTTP/API 통합 테스트 | `{Context}IntegrationTest` | `Controller` suffix는 테스트 클래스명에서 제거한다. 예: `PostIntegrationTest`, `AuthIntegrationTest` |
| API version이 같은 context 안에서 분리된 경우 | `{Context}V{N}IntegrationTest` | 예: `PostV2IntegrationTest` |
| 이벤트/스케줄러/동시성/after-commit 통합 테스트 | 목적이 드러나는 suffix 허용 | 예: `ReadPostCommandServiceConcurrencyIntegrationTest`, `UserAccountAfterCommitEventIntegrationTest` |

### 메서드명

- 테스트 메서드는 `subject_condition_expectedResult` 형태를 기본으로 한다.
  - 예: `getBookmarks_Success_WithCursor`, `findByIdWithTechBlog_NotFound_ReturnsEmpty`
- 행위가 `@Nested`로 충분히 묶였으면 condition/expectedResult 중심으로 짧게 써도 된다.
  - 예: `withCookie_ReturnsDeserializedAuthorizationRequest`
- 단순 성공/실패만 표현하는 `*_Success`, `*_Fail`은 새 테스트에서 지양한다. 조건이나 관찰 결과를 함께 쓴다.
- 기존 테스트를 대량 rename하지 않는다. 기능 변경/테스트 보강 시 해당 파일 안에서 자연스럽게 맞춘다.

### `@Nested` / `@DisplayName`

- `@Nested`는 행위, API endpoint, 시나리오 단위로 묶는다.
  - 예: `GET /api/v1/posts/{postId}`, `북마크 목록 조회`, `loadUser`
- `Success`/`Failure`만 단독 최상위 그룹으로 남발하지 않는다. 더 큰 행위 그룹 아래에서 의미가 있을 때만 사용한다.
- `@DisplayName`은 관찰 가능한 동작이나 기대 결과가 드러나도록 한글로 작성하는 것을 기본으로 한다.
- HTTP/API 통합 테스트의 `@DisplayName`은 wire contract가 드러나도록 endpoint, 인증 여부, 응답 상태/필드를 표현한다.

## Fixture 컨벤션

- 여러 테스트 클래스에서 재사용하는 fixture는 소유 context의 `fixture` 패키지에 둔다.
- 한 테스트 클래스에서만 쓰는 setup은 private helper method로 유지한다.
- 서로 관련 없는 도메인을 묶는 global fixture 패키지는 만들지 않는다.
- 순수 객체 fixture와 repository 저장 helper는 역할을 분리한다.
- fixture에서는 `LocalDateTime.now()`보다 안정적인 기본 시간값을 우선 사용한다.
- `ReflectionTestUtils`는 production factory가 의도적으로 숨긴 ID/state를 테스트에서 주입해야 할 때만 사용한다.

## 통합 테스트 컨벤션

- 일반 통합 테스트 base는 `MySqlRedisIntegrationTestBase`를 기본으로 사용한다.
- `MySqlRedisIntegrationTestBase`는 MySQL/Redis 컨테이너만 띄우고 Elasticsearch 관련 bean/repository는 mock 처리한다.
- Elasticsearch 실기동이 필요한 평가/검색 품질 검증은 일반 `integrationTest` 레인과 섞지 않고 별도 base/레인에서 실행한다.
- 같은 `integrationTest` 레인 안에서는 base/config 조합을 불필요하게 나누지 않는다. Spring context cache key가 달라지면 Testcontainers lifecycle이 갈라져 CI 실행 시간이 늘어난다.

## 실행 명령

```bash
./gradlew test -PexcludeIntegration
./gradlew integrationTest
./gradlew evaluationTest
./gradlew evaluationSetup
```
