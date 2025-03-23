package com.distlock.lock;

/**
 * Interface defining distributed lock operations
 */
public interface DistributedLock {

    /**
     * Acquires a lock with the specified key
     *
     * @param lockKey the key to lock
     * @return true if lock was acquired successfully, false otherwise
     */
    boolean acquire(String lockKey);

    /**
     * Acquires a lock with the specified key and a timeout
     *
     * @param lockKey the key to lock
     * @param timeoutMs maximum time to wait for lock in milliseconds
     * @return true if lock was acquired successfully, false otherwise
     */
    boolean acquire(String lockKey, long timeoutMs);

    /**
     * Releases a previously acquired lock
     *
     * @param lockKey the key to unlock
     * @return true if lock was released successfully, false otherwise
     */
    boolean release(String lockKey);

    /**
     * Checks if a lock is currently held
     *
     * @param lockKey the key to check
     * @return true if the lock is held, false otherwise
     */
    boolean isLocked(String lockKey);

    /**
     * Gets the type of lock implementation
     *
     * @return the lock type
     */
    String getType();
}