package com.techfork.global.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis 기반 분산 락 구현
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLock {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String LOCK_PREFIX = "lock:";

    /**
     * 락 획득 시도
     *
     * @param key 락 키
     * @param ttl 락 유지 시간
     * @return 락 획득 성공 시 락 식별자, 실패 시 null
     */
    public String tryLock(String key, Duration ttl) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, ttl);

        if (Boolean.TRUE.equals(success)) {
            log.debug("Lock acquired: key={}, value={}", lockKey, lockValue);
            return lockValue;
        }

        log.debug("Lock acquisition failed: key={}", lockKey);
        return null;
    }

    /**
     * 락 해제
     *
     * @param key 락 키
     * @param lockValue 락 획득 시 받은 식별자
     * @return 락 해제 성공 여부
     */
    public boolean unlock(String key, String lockValue) {
        if (lockValue == null) {
            return false;
        }

        String lockKey = LOCK_PREFIX + key;
        String currentValue = redisTemplate.opsForValue().get(lockKey);

        if (lockValue.equals(currentValue)) {
            Boolean deleted = redisTemplate.delete(lockKey);
            log.debug("Lock released: key={}, success={}", lockKey, deleted);
            return Boolean.TRUE.equals(deleted);
        }

        log.warn("Lock value mismatch: key={}, expected={}, current={}", lockKey, lockValue, currentValue);
        return false;
    }

    /**
     * 락 획득을 기다리며 시도
     *
     * @param key 락 키
     * @param ttl 락 유지 시간
     * @param waitTime 대기 시간
     * @param retryInterval 재시도 간격
     * @return 락 획득 성공 시 락 식별자, 실패 시 null
     */
    public String tryLockWithWait(String key, Duration ttl, Duration waitTime, Duration retryInterval) {
        long startTime = System.currentTimeMillis();
        long waitMillis = waitTime.toMillis();
        long retryMillis = retryInterval.toMillis();

        while (System.currentTimeMillis() - startTime < waitMillis) {
            String lockValue = tryLock(key, ttl);
            if (lockValue != null) {
                return lockValue;
            }

            try {
                Thread.sleep(retryMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Lock acquisition interrupted: key={}", key, e);
                return null;
            }
        }

        log.warn("Lock acquisition timeout: key={}, waitTime={}", key, waitTime);
        return null;
    }
}
