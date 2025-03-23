package com.distlock.lock;

import com.distlock.config.ZookeeperConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ZookeeperLock implements DistributedLock {

    private final CuratorFramework curatorClient;
    private final ZookeeperConfig zookeeperConfig;
    private final ConcurrentHashMap<String, InterProcessMutex> lockMap = new ConcurrentHashMap<>();

    @Autowired
    public ZookeeperLock(CuratorFramework curatorClient, ZookeeperConfig zookeeperConfig) {
        this.curatorClient = curatorClient;
        this.zookeeperConfig = zookeeperConfig;
    }

    @Override
    public boolean acquire(String lockKey) {
        return acquire(lockKey, zookeeperConfig.getLock().getWaitTime());
    }

    @Override
    public boolean acquire(String lockKey, long timeoutMs) {
        String lockPath = zookeeperConfig.getLock().getBasePath() + "/" + lockKey;

        try {
            // Ensure the base path exists
            try {
                curatorClient.create()
                        .creatingParentsIfNeeded()
                        .forPath(zookeeperConfig.getLock().getBasePath());
            } catch (KeeperException.NodeExistsException ignore) {
                // Base path already exists, which is fine
            }

            InterProcessMutex lock = new InterProcessMutex(curatorClient, lockPath);
            lockMap.put(lockKey, lock);

            boolean acquired = lock.acquire(timeoutMs, TimeUnit.MILLISECONDS);
            if (acquired) {
                log.debug("Successfully acquired ZooKeeper lock: {}", lockPath);
            } else {
                log.debug("Failed to acquire ZooKeeper lock: {}", lockPath);
                lockMap.remove(lockKey);
            }

            return acquired;
        } catch (Exception e) {
            log.error("Error acquiring ZooKeeper lock: {}", lockPath, e);
            lockMap.remove(lockKey);
            return false;
        }
    }

    @Override
    public boolean release(String lockKey) {
        InterProcessMutex lock = lockMap.get(lockKey);
        if (lock == null) {
            log.warn("Cannot release ZooKeeper lock, not found in map: {}", lockKey);
            return false;
        }

        try {
            lock.release();
            lockMap.remove(lockKey);
            log.debug("Successfully released ZooKeeper lock: {}", lockKey);
            return true;
        } catch (Exception e) {
            log.error("Error releasing ZooKeeper lock: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public boolean isLocked(String lockKey) {
        InterProcessMutex lock = lockMap.get(lockKey);

        if (lock == null) {
            return false;
        }

        try {
            String lockPath = zookeeperConfig.getLock().getBasePath() + "/" + lockKey;
            return curatorClient.checkExists().forPath(lockPath) != null;
        } catch (Exception e) {
            log.error("Error checking ZooKeeper lock: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public String getType() {
        return "Zookeeper";
    }
}