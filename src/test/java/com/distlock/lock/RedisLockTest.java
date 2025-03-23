package com.distlock.lock;

import com.distlock.config.RedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RedisLockTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisConfig redisConfig;
    private RedisLock redisLock;

    @BeforeEach
    public void setup() {
        redisConfig = new RedisConfig();
        RedisConfig.Lock lockConfig = new RedisConfig.Lock();
        lockConfig.setTtl(30000);
        lockConfig.setRetryInterval(100);
        lockConfig.setRetryTimes(3);
        redisConfig.setLock(lockConfig);



        redisLock = new RedisLock(redisTemplate, redisConfig);
    }

    @Test
    public void testAcquireLockSuccess() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String lockKey = "test-lock";
        when(valueOperations.setIfAbsent(
                eq("lock:" + lockKey),
                anyString(),
                anyLong(),
                eq(TimeUnit.MILLISECONDS))
        ).thenReturn(true);

        // Act
        boolean result = redisLock.acquire(lockKey);

        // Assert
        assertTrue(result);
    }

    @Test
    public void testAcquireLockFailure() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String lockKey = "test-lock";
        when(valueOperations.setIfAbsent(
                eq("lock:" + lockKey),
                anyString(),
                anyLong(),
                eq(TimeUnit.MILLISECONDS))
        ).thenReturn(false);

        // Act
        boolean result = redisLock.acquire(lockKey);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testReleaseLockSuccess() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Arrange
        String lockKey = "test-lock";

        // First acquire the lock to set the thread local value
        when(valueOperations.setIfAbsent(
                eq("lock:" + lockKey),
                anyString(),
                anyLong(),
                eq(TimeUnit.MILLISECONDS))
        ).thenReturn(true);

        boolean acquired = redisLock.acquire(lockKey);
        assertTrue(acquired);

        // Mock the Lua script execution for release
        when(redisTemplate.execute(
                any(RedisScript.class),
                eq(Collections.singletonList("lock:" + lockKey)),
                anyString())
        ).thenReturn(1L);

        // Act
        boolean result = redisLock.release(lockKey);

        // Assert
        assertTrue(result);
    }

    @Test
    public void testIsLocked() {
        // Arrange
        String lockKey = "test-lock";
        when(redisTemplate.hasKey(eq("lock:" + lockKey))).thenReturn(true);

        // Act
        boolean result = redisLock.isLocked(lockKey);

        // Assert
        assertTrue(result);
    }
}