
    

package com.payswitch.concurrency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class AccountLockManager {

    private final ConcurrentHashMap<Long, ReentrantLock> accountLocks = new ConcurrentHashMap<>();

    public ReentrantLock getLockForAccount(long accountId) {
        return accountLocks.computeIfAbsent(accountId, id -> new ReentrantLock());
    }

    public void lockAccounts(long accountId1, long accountId2) {
        if (accountId1 == accountId2) {
            getLockForAccount(accountId1).lock();
            return;
        }

        long firstLockId = Math.min(accountId1, accountId2);
        long secondLockId = Math.max(accountId1, accountId2);

        getLockForAccount(firstLockId).lock();
        getLockForAccount(secondLockId).lock();
    }

    public void unlockAccounts(long accountId1, long accountId2) {
        if (accountId1 == accountId2) {
            ReentrantLock lock = getLockForAccount(accountId1);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            return;
        }

        long firstLockId = Math.min(accountId1, accountId2);
        long secondLockId = Math.max(accountId1, accountId2);

        ReentrantLock secondLock = getLockForAccount(secondLockId);
        ReentrantLock firstLock = getLockForAccount(firstLockId);

        if (secondLock.isHeldByCurrentThread()) {
            secondLock.unlock();
        }
        if (firstLock.isHeldByCurrentThread()) {
            firstLock.unlock();
        }
    }
}