package com.techfork.auth.security.cache;

import com.techfork.auth.security.oauth.UserPrincipal;
import com.techfork.useraccount.application.auth.UserAuthProfile;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.UserStatus;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAuthCacheStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserAuthCacheStore userAuthCacheStore;

    private static final Long USER_ID = 1L;
    private static final String CACHE_KEY = "user:auth:" + USER_ID;
    private static final String ACTIVE_USER_CACHE_VALUE = "1|USER|ACTIVE|test@example.com";
    private static final String WITHDRAWN_USER_CACHE_VALUE = "1|USER|WITHDRAWN|";

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("캐시 미스면 null을 반환한다")
        void cacheMiss_ReturnsNull() {
            // Given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(CACHE_KEY)).willReturn(null);

            // When
            UserPrincipal result = userAuthCacheStore.get(USER_ID);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("캐시 히트면 기존 wire format을 역직렬화해 UserPrincipal을 반환한다")
        void cacheHit_ReturnsDeserializedPrincipal() {
            // Given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(CACHE_KEY)).willReturn(ACTIVE_USER_CACHE_VALUE);

            // When
            UserPrincipal result = userAuthCacheStore.get(USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getRole()).isEqualTo(Role.USER);
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("빈 email은 null email로 역직렬화한다")
        void cacheHitWithEmptyEmail_ReturnsNullEmail() {
            // Given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(CACHE_KEY)).willReturn(WITHDRAWN_USER_CACHE_VALUE);

            // When
            UserPrincipal result = userAuthCacheStore.get(USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(result.getEmail()).isNull();
            assertThat(result.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("ACTIVE 상태는 enabled principal로 역직렬화한다")
        void cacheHitWithActiveStatus_ReturnsEnabledPrincipal() {
            // Given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(CACHE_KEY)).willReturn(ACTIVE_USER_CACHE_VALUE);

            // When
            UserPrincipal result = userAuthCacheStore.get(USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("잘못된 포맷이면 null을 반환한다")
        void invalidFormat_ReturnsNull() {
            // Given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(CACHE_KEY)).willReturn("invalid-format");

            // When
            UserPrincipal result = userAuthCacheStore.get(USER_ID);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("put")
    class Put {

        @Test
        @DisplayName("인증 프로필을 기존 wire format으로 직렬화해 저장한다")
        void authProfile_SerializesAndSaves() {
            // Given
            UserAuthProfile userAuthProfile = new UserAuthProfile(
                    USER_ID,
                    Role.ADMIN,
                    UserStatus.ACTIVE,
                    "admin@example.com",
                    true
            );

            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // When
            userAuthCacheStore.put(USER_ID, userAuthProfile, 180000L);

            // Then
            verify(valueOperations).set(CACHE_KEY, "1|ADMIN|ACTIVE|admin@example.com", 180000L, TimeUnit.MILLISECONDS);
        }

        @Test
        @DisplayName("email이 null이면 빈 email로 저장한다")
        void nullEmail_SerializesEmptyEmail() {
            // Given
            UserAuthProfile userAuthProfile = new UserAuthProfile(
                    USER_ID,
                    Role.USER,
                    UserStatus.PENDING,
                    null,
                    false
            );

            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // When
            userAuthCacheStore.put(USER_ID, userAuthProfile, 180000L);

            // Then
            verify(valueOperations).set(CACHE_KEY, "1|USER|PENDING|", 180000L, TimeUnit.MILLISECONDS);
        }
    }

    @Nested
    @DisplayName("evict")
    class Evict {

        @Test
        @DisplayName("사용자 인증 캐시 키를 삭제한다")
        void existingKey_DeletesCacheKey() {
            // When
            userAuthCacheStore.evict(USER_ID);

            // Then
            verify(redisTemplate).delete(CACHE_KEY);
        }
    }
}
