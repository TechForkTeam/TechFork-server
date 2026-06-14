package com.techfork.auth.security.service;

import com.techfork.global.constant.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    public void saveRefreshToken(Long userId, String token, Long expiration) {
        String key = RedisKey.REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, token, expiration, TimeUnit.MILLISECONDS);
        log.info("RefreshToken saved for userId: {}", userId);
    }

    public boolean validateRefreshToken(Long userId, String token) {
        String key = RedisKey.REFRESH_TOKEN_PREFIX + userId;
        String storedToken = redisTemplate.opsForValue().get(key);

        return storedToken != null && storedToken.equals(token);
    }

    public void deleteRefreshToken(Long userId) {
        String key = RedisKey.REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("RefreshToken deleted for userId: {}", userId);
    }
}