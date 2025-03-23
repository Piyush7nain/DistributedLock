package com.distlock.service;

import com.distlock.config.AppConfig;
import com.distlock.lock.DistributedLock;
import com.distlock.lock.RedisLock;
import com.distlock.lock.ZookeeperLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class LockService {

    private final RedisLock redisLock;
    private final ZookeeperLock zookeeperLock;
    private final AppConfig appConfig;

    @Autowired
    public LockService(RedisLock redisLock, ZookeeperLock zookeeperLock, AppConfig appConfig) {
        this.redisLock = redisLock;
        this.zookeeperLock = zookeeperLock;
        this.appConfig = appConfig;
    }

    /**
     * Acquires a lock using the configured strategy
     *
     * @param lockKey the key to lock
     * @return true if lock was acquired, false otherwise
     */
    public boolean acquireLock(String lockKey) {
        if ("both".equals(appConfig.getStrategy())) {
            // For 'both' strategy, we try Redis first, then ZooKeeper if Redis fails
            if (redisLock.acquire(lockKey)) {
                // If Redis lock succeeds, try ZooKeeper lock
                if (zookeeperLock.acquire(lockKey)) {
                    return true;
                } else {
                    // If ZooKeeper fails, release Redis lock and return false
                    redisLock.release(lockKey);
                    return false;
                }
            } else {
                // If Redis lock fails, don't even try ZooKeeper
                return false;
            }
        } else if ("redis".equals(appConfig.getStrategy())) {
            return redisLock.acquire(lockKey);
        } else if ("zookeeper".equals(appConfig.getStrategy())) {
            return zookeeperLock.acquire(lockKey);
        } else {
            log.error("Invalid lock strategy: {}", appConfig.getStrategy());
            return false;
        }
    }

    /**
     * Acquires a lock with a timeout using the configured strategy
     *
     * @param lockKey the key to lock
     * @param timeoutMs the timeout in milliseconds
     * @return true if lock was acquired, false otherwise
     */
    public boolean acquireLock(String lockKey, long timeoutMs) {
        if ("both".equals(appConfig.getStrategy())) {
            // For 'both' strategy, we try Redis first, then ZooKeeper if Redis fails
            if (redisLock.acquire(lockKey, timeoutMs)) {
                // If Redis lock succeeds, try ZooKeeper lock
                if (zookeeperLock.acquire(lockKey, timeoutMs)) {
                    return true;
                } else {
                    // If ZooKeeper fails, release Redis lock and return false
                    redisLock.release(lockKey);
                    return false;
                }
            } else {
                // If Redis lock fails, don't even try ZooKeeper
                return false;
            }
        } else if ("redis".equals(appConfig.getStrategy())) {
            return redisLock.acquire(lockKey, timeoutMs);
        } else if ("zookeeper".equals(appConfig.getStrategy())) {
            return zookeeperLock.acquire(lockKey, timeoutMs);
        } else {
            log.error("Invalid lock strategy: {}", appConfig.getStrategy());
            return false;
        }
    }

    /**
     * Releases a lock using the configured strategy
     *
     * @param lockKey the key to unlock
     * @return true if lock was released, false otherwise
     */
    public boolean releaseLock(String lockKey) {
        boolean result = true;

        if (appConfig.useRedis()) {
            result = result && redisLock.release(lockKey);
        }

        if (appConfig.useZookeeper()) {
            result = result && zookeeperLock.release(lockKey);
        }

        return result;
    }

    /**
     * Checks if a lock is held using the configured strategy
     *
     * @param lockKey the key to check
     * @return true if lock is held, false otherwise
     */
    public boolean isLocked(String lockKey) {
        if ("both".equals(appConfig.getStrategy())) {
            return redisLock.isLocked(lockKey) && zookeeperLock.isLocked(lockKey);
        } else if ("redis".equals(appConfig.getStrategy())) {
            return redisLock.isLocked(lockKey);
        } else if ("zookeeper".equals(appConfig.getStrategy())) {
            return zookeeperLock.isLocked(lockKey);
        } else {
            log.error("Invalid lock strategy: {}", appConfig.getStrategy());
            return false;
        }
    }

    /**
     * Gets information about the lock system
     *
     * @return Map with information about the lock system
     */
    public Map<String, Object> getLockInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("strategy", appConfig.getStrategy());
        info.put("usingRedis", appConfig.useRedis());
        info.put("usingZookeeper", appConfig.useZookeeper());
        return info;
    }
}