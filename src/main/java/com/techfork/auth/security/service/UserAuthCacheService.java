package com.techfork.auth.security.service;

import com.techfork.useraccount.application.auth.UserAuthProfile;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.UserStatus;
import com.techfork.global.constant.RedisKey;
import com.techfork.auth.security.oauth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthCacheService {

    private static final String DELIMITER = "|";
    private static final int FIELD_COUNT = 4;

    private final StringRedisTemplate redisTemplate;

    public UserPrincipal get(Long userId) {
        String key = buildKey(userId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            return null;
        }
        return deserialize(cached);
    }

    public void put(Long userId, User user, long ttlMillis) {
        put(userId, UserAuthProfile.from(user), ttlMillis);
    }

    public void put(Long userId, UserAuthProfile userAuthProfile, long ttlMillis) {
        String key = buildKey(userId);
        String value = serialize(userAuthProfile);
        redisTemplate.opsForValue().set(key, value, ttlMillis, TimeUnit.MILLISECONDS);
        log.debug("User auth cached for userId: {}", userId);
    }

    public void evict(Long userId) {
        String key = buildKey(userId);
        redisTemplate.delete(key);
        log.debug("User auth cache evicted for userId: {}", userId);
    }

    private String buildKey(Long userId) {
        return RedisKey.USER_AUTH_PREFIX + userId;
    }

    private String serialize(UserAuthProfile userAuthProfile) {
        return userAuthProfile.id()
                + DELIMITER + userAuthProfile.role().name()
                + DELIMITER + userAuthProfile.status().name()
                + DELIMITER + (userAuthProfile.email() != null ? userAuthProfile.email() : "");
    }

    private UserPrincipal deserialize(String value) {
        String[] parts = value.split("\\" + DELIMITER, FIELD_COUNT);
        if (parts.length != FIELD_COUNT) {
            log.warn("Invalid user auth cache format: {}", value);
            return null;
        }
        return UserPrincipal.builder()
                .id(Long.parseLong(parts[0]))
                .role(Role.valueOf(parts[1]))
                .status(UserStatus.valueOf(parts[2]))
                .email(parts[3].isEmpty() ? null : parts[3])
                .build();
    }
}
