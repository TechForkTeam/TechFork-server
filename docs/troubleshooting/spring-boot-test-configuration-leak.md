# SpringBootTest에서 테스트 전용 설정이 다른 통합 테스트 설정을 덮어쓴 문제

## 배경

일반 통합 테스트는 Elasticsearch를 직접 사용하지 않는데도 기존에는 `IntegrationTestBase`를 상속하면서 MySQL, Redis, Elasticsearch Testcontainer를 모두 기동했다.

CI에서는 Elasticsearch 컨테이너 시작이 timeout 되면서 Elasticsearch를 쓰지 않는 Auth, Bookmark 같은 통합 테스트까지 함께 실패했다.

이를 줄이기 위해 일반 API 통합 테스트는 `MySqlRedisIntegrationTestBase`를 사용하도록 정리했다.

## 증상

`MySqlRedisIntegrationTestBase`로 전환한 뒤에도 일부 테스트에서 이상한 현상이 발생했다.

대표 증상은 다음과 같았다.

- `@ServiceConnection`이 있는데도 실제 datasource가 MySQL Testcontainer가 아니라 H2로 잡힘
- `userRepository.save(...)` 이후에도 `id`가 `null`
- `RefreshTokenStore` 로그에 `userId: null`
- Auth refresh API에서 `The given id must not be null`로 500 발생
- Hibernate schema validation 또는 JPA 저장 흐름이 예상과 다르게 동작

진단용 probe에서는 다음과 같은 상태가 확인됐다.

```text
PROBE url=jdbc:h2:mem:...
PROBE db=H2
PROBE txManager=DataSourceTransactionManager
PROBE save id=null count=0
```

즉, MySQL/Redis Testcontainer는 떠 있었지만 실제 Spring ApplicationContext는 H2 datasource를 사용하고 있었다.

## 원인

원인은 `PersonalizedProfileGeneratedAfterCommitIntegrationTest` 내부 테스트 설정이었다.

```java
@SpringJUnitConfig(PersonalizedProfileGeneratedAfterCommitIntegrationTest.TestConfig.class)
class PersonalizedProfileGeneratedAfterCommitIntegrationTest {

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
```

이 설정은 해당 테스트에서만 쓰려던 테스트 전용 설정이었지만 `@Configuration`으로 선언되어 있었다.

그 결과 다른 `@SpringBootTest` 컨텍스트가 테스트 classpath를 스캔할 때 이 내부 설정까지 component scan 대상으로 잡았다.

그러면서 H2 `DataSource`와 `DataSourceTransactionManager`가 전역 테스트 컨텍스트에 등록되었고, `@ServiceConnection`이 제공하려던 MySQL Testcontainer datasource 설정이 실사용 datasource로 연결되지 않았다.

정리하면 다음과 같다.

```text
테스트 전용 H2 설정
        ↓
@Configuration 때문에 component scan 대상이 됨
        ↓
다른 @SpringBootTest 컨텍스트에도 H2 DataSource 등록
        ↓
@ServiceConnection 기반 MySQL datasource가 실사용되지 않음
        ↓
JPA 저장/트랜잭션/스키마 검증 흐름이 깨짐
```

`@ServiceConnection` 자체가 문제였던 것은 아니다.

오히려 `@ServiceConnection`은 정상 동작할 수 있었지만, 테스트 전용 `@Configuration`이 먼저 또는 함께 등록되면서 설정이 덮어써진 것이 문제였다.

## 해결

테스트 내부 전용 설정을 `@Configuration`에서 `@TestConfiguration`으로 변경했다.

```java
@TestConfiguration
@EnableTransactionManagement
static class TestConfig {
    // test-only beans
}
```

`@TestConfiguration`은 테스트 전용 설정을 표현하는 어노테이션이며, 일반 component scan 대상에서 제외된다.

따라서 `@SpringJUnitConfig(TestConfig.class)`처럼 명시적으로 로드한 테스트에서는 계속 사용할 수 있고, 다른 `@SpringBootTest` 컨텍스트에는 새어 들어가지 않는다.

## 함께 정리한 내용

일반 통합 테스트는 Elasticsearch에 의존하지 않도록 `MySqlRedisIntegrationTestBase`를 기본으로 사용하도록 정리했다.

- 일반 API 통합 테스트: `MySqlRedisIntegrationTestBase`
- Elasticsearch 실기동이 필요한 평가/검색 품질 테스트: `IntegrationTestBase`

이를 통해 Elasticsearch Testcontainer startup timeout이 일반 통합 테스트 전체를 실패시키는 문제를 줄였다.

## 검증

다음 검증을 수행했다.

```bash
./gradlew integrationTest
```

결과: 성공

개별 확인:

```bash
./gradlew integrationTest --tests 'com.techfork.auth.integration.AuthIntegrationTest'
./gradlew integrationTest --tests 'com.techfork.activity.bookmark.integration.BookmarkIntegrationTest'
```

결과: 성공

추가로 로그에서 Spring Batch가 실제 MySQL metadata를 인식하는 것도 확인했다.

```text
JobRepositoryFactoryBean - No database type set, using meta data indicating: MYSQL
```

## 재발 방지

테스트 전용 설정을 만들 때는 다음 기준을 따른다.

- 특정 테스트에서만 쓰는 설정은 `@Configuration` 대신 `@TestConfiguration`을 사용한다.
- `@SpringBootTest` 전체 스캔 대상에 테스트 전용 `DataSource`, `TransactionManager`가 노출되지 않게 한다.
- `@ServiceConnection`을 쓰는 Testcontainer 기반 통합 테스트에서는 datasource를 수동으로 덮어쓰지 않는다.
- 통합 테스트 base를 나눌 때는 Spring context cache key와 Testcontainer lifecycle이 달라질 수 있음을 확인한다.

## 결론

이번 문제는 Testcontainers나 `@ServiceConnection`의 문제가 아니라, 테스트 전용 H2 설정이 `@Configuration`으로 선언되어 다른 통합 테스트 컨텍스트까지 오염시킨 문제였다.

`@TestConfiguration`으로 변경해 설정 누수를 막으면서 `@ServiceConnection` 기반 MySQL/Redis Testcontainer 연결이 정상 복구되었다.
