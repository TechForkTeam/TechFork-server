package com.techfork.global.security.auth.service;

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
}