package com.techfork.auth.security.service;

import com.techfork.auth.security.oauth.UserPrincipal;
import com.techfork.useraccount.application.auth.UserAuthProfile;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.UserStatus;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
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
class UserAuthCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserAuthCacheService userAuthCacheService;

    private static final Long USER_ID = 1L;
    private static final String CACHE_KEY = "user:auth:" + USER_ID;

    // ===== get 테스트 =====

    @Test
    @DisplayName("get - 캐시 미스: null 반환")
    void get_CacheMiss_ReturnsNull() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY)).willReturn(null);

        // When
        UserPrincipal result = userAuthCacheService.get(USER_ID);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("get - 캐시 히트: 역직렬화 후 UserPrincipal 반환")
    void get_CacheHit_ReturnsDeserializedPrincipal() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY)).willReturn("1|USER|ACTIVE|test@example.com");

        // When
        UserPrincipal result = userAuthCacheService.get(USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRole()).isEqualTo(Role.USER);
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("get - 캐시 히트: email이 빈 문자열이면 null로 역직렬화")
    void get_CacheHit_EmptyEmail_ReturnsNullEmail() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY)).willReturn("1|USER|WITHDRAWN|");

        // When
        UserPrincipal result = userAuthCacheService.get(USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(result.getEmail()).isNull();
    }

    @Test
    @DisplayName("get - 잘못된 포맷: null 반환")
    void get_InvalidFormat_ReturnsNull() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY)).willReturn("invalid-format");

        // When
        UserPrincipal result = userAuthCacheService.get(USER_ID);

        // Then
        assertThat(result).isNull();
    }

    // ===== put 테스트 =====

    @Test
    @DisplayName("put - 인증 프로필 직렬화 후 저장")
    void put_WithAuthProfile_SerializesAndSaves() {
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
        userAuthCacheService.put(USER_ID, userAuthProfile, 180000L);

        // Then
        verify(valueOperations).set(CACHE_KEY, "1|ADMIN|ACTIVE|admin@example.com", 180000L, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("put - email이 null인 인증 프로필은 빈 email로 저장")
    void put_WithNullEmail_SerializesEmptyEmail() {
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
        userAuthCacheService.put(USER_ID, userAuthProfile, 180000L);

        // Then
        verify(valueOperations).set(CACHE_KEY, "1|USER|PENDING|", 180000L, TimeUnit.MILLISECONDS);
    }

    // ===== evict 테스트 =====

    @Test
    @DisplayName("evict - 캐시 키 삭제")
    void evict_DeletesCacheKey() {
        // When
        userAuthCacheService.evict(USER_ID);

        // Then
        verify(redisTemplate).delete(CACHE_KEY);
    }
}
