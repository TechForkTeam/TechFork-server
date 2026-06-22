# 테스트 컨벤션

## 패키지 컨벤션

- 테스트 패키지는 `com.techfork.<bounded-context>.<test-kind>`를 기본으로 한다.
  - 예: `post.domain`, `post.infrastructure`, `post.integration`, `useraccount.presentation.request`, `auth.integration`
- `test-kind` 패키지는 테스트의 역할을 드러내는 이름을 사용한다.
  - 예: `domain`, `application`, `infrastructure`, `presentation`, `integration`, `fixture`
- production 패키지가 아직 `com.techfork.domain.*`에 남아 있는 영역은 테스트도 legacy root를 유지한다.
- legacy slice를 옮길 때는 production과 test를 같은 작업 단위에서 함께 옮긴다. 네이밍 정리만을 위해 테스트만 먼저 이동하지 않는다.
- `src/test/java/com/techfork/evaluation`은 별도 evaluation lane이다. 해당 하위 패키지는 [로컬 가이드](../../src/test/java/com/techfork/evaluation/AGENTS.md)를 따른다.

## 네이밍 컨벤션

- Repository 검증 테스트는 `{RepositoryName}Test`를 사용한다.
- 같은 subject에 일반 단위 테스트와 persistence slice 테스트가 함께 있을 때만 `{Subject}DataJpaTest`를 사용한다.
- HTTP/API 통합 테스트는 `{Context}IntegrationTest`를 기본으로 한다. 더 좁은 시나리오가 명확하다면 목적이 드러나는 suffix를 사용할 수 있다.
- 이벤트, 스케줄러, 동시성, after-commit 테스트는 목적이 드러나는 suffix를 사용할 수 있다.
- `@Nested`는 행위나 시나리오 단위로 묶는다. `Success`/`Failure`만 단독으로 남발하지 말고, 더 큰 행위 그룹 아래에서 의미가 있을 때 사용한다.
- `@DisplayName`은 관찰 가능한 동작이나 기대 결과가 드러나도록 한글로 작성하는 것을 기본으로 한다.

## Fixture 컨벤션

- 여러 테스트 클래스에서 재사용하는 fixture는 소유 context의 `fixture` 패키지에 둔다.
- 한 테스트 클래스에서만 쓰는 setup은 private helper method로 유지한다.
- 서로 관련 없는 도메인을 묶는 global fixture 패키지는 만들지 않는다.
- 순수 객체 fixture와 repository 저장 helper는 역할을 분리한다.
- fixture에서는 `LocalDateTime.now()`보다 안정적인 기본 시간값을 우선 사용한다.
- `ReflectionTestUtils`는 production factory가 의도적으로 숨긴 ID/state를 테스트에서 주입해야 할 때만 사용한다.

## 통합 테스트 컨벤션

- 통합 테스트 base는 `IntegrationTestBase`를 기본으로 사용한다.
- `MySqlRedisIntegrationTestBase`로 대량 전환하지 않는다. base/config 조합이 달라지면 Spring context cache key와 Testcontainers lifecycle이 CI에서 갈라질 수 있다.
- integration base를 분리하려면 먼저 container 재사용 여부와 CI 실행 시간 영향을 검증한다.

## 실행 명령

```bash
./gradlew test -PexcludeIntegration
./gradlew integrationTest
./gradlew evaluationTest
./gradlew evaluationSetup
```
