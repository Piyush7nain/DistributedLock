package com.distlock.lock;

import com.distlock.config.RedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisLock implements DistributedLock {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisConfig redisConfig;
    private final ThreadLocal<String> lockValueThreadLocal = new ThreadLocal<>();

    // Lua script for lock release (ensures we only delete our own lock)
    private static final String RELEASE_LOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('del', KEYS[1]) " +
                    "else " +
                    "    return 0 " +
                    "end";

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT, Long.class);

    @Autowired
    public RedisLock(RedisTemplate<String, String> redisTemplate, RedisConfig redisConfig) {
        this.redisTemplate = redisTemplate;
        this.redisConfig = redisConfig;
    }

    @Override
    public boolean acquire(String lockKey) {
        return acquire(lockKey, redisConfig.getLock().getTtl());
    }

    @Override
    public boolean acquire(String lockKey, long timeoutMs) {
        final String lockValue = UUID.randomUUID().toString();
        final long startTime = System.currentTimeMillis();
        final int retryTimes = redisConfig.getLock().getRetryTimes();
        final long retryInterval = redisConfig.getLock().getRetryInterval();

        lockKey = "lock:" + lockKey;

        try {
            int retryCount = 0;
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, timeoutMs, TimeUnit.MILLISECONDS);

                if (Boolean.TRUE.equals(result)) {
                    lockValueThreadLocal.set(lockValue);
                    log.debug("Successfully acquired Redis lock: {}", lockKey);
                    return true;
                }

                retryCount++;
                if (retryCount > retryTimes) {
                    log.debug("Failed to acquire Redis lock after {} retries: {}", retryTimes, lockKey);
                    return false;
                }

                log.debug("Waiting for Redis lock: {}, retry: {}/{}", lockKey, retryCount, retryTimes);
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            log.debug("Timeout waiting for Redis lock: {}", lockKey);
            return false;
        } catch (Exception e) {
            log.error("Error acquiring Redis lock: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public boolean release(String lockKey) {
        lockKey = "lock:" + lockKey;
        String lockValue = lockValueThreadLocal.get();

        if (lockValue == null) {
            log.warn("Cannot release Redis lock, no value found in ThreadLocal: {}", lockKey);
            return false;
        }

        try {
            Long result = redisTemplate.execute(
                    RELEASE_SCRIPT,
                    Collections.singletonList(lockKey),
                    lockValue
            );

            boolean released = result != null && result == 1L;
            if (released) {
                lockValueThreadLocal.remove();
                log.debug("Successfully released Redis lock: {}", lockKey);
            } else {
                log.warn("Failed to release Redis lock: {}", lockKey);
            }

            return released;
        } catch (Exception e) {
            log.error("Error releasing Redis lock: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public boolean isLocked(String lockKey) {
        lockKey = "lock:" + lockKey;
        try {
            return redisTemplate.hasKey(lockKey);
        } catch (Exception e) {
            log.error("Error checking Redis lock: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public String getType() {
        return "Redis";
    }
}