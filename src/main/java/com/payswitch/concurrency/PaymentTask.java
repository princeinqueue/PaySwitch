package com.payswitch.concurrency;

import java.math.BigDecimal;
import java.util.concurrent.Callable;

/**
 * Represents a single concurrent payment transfer task.
 * Implements Callable to return a result/status and Comparable for PriorityQueue ordering.
 */
public class PaymentTask implements Callable<String>, Comparable<PaymentTask> {

    @FunctionalInterface
    public interface TransferOperation {
        String execute(long senderId, long receiverId, BigDecimal amount) throws Exception;
    }

    private final String taskId;
    private final long senderId;
    private final long receiverId;
    private final BigDecimal amount;
    private final int priority; // Lower number = Higher priority (e.g., 1 = Urgent, 2 = Normal)
    private final AccountLockManager lockManager;
    private final TransferOperation operation;

    public PaymentTask(String taskId, long senderId, long receiverId, BigDecimal amount,
                       int priority, AccountLockManager lockManager, TransferOperation operation) {
        this.taskId = taskId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.priority = priority;
        this.lockManager = lockManager;
        this.operation = operation;
    }

    @Override
    public String call() throws Exception {
        // Acquire locks in ascending accountId order to prevent deadlocks
        lockManager.lockAccounts(senderId, receiverId);
        try {
            return operation.execute(senderId, receiverId, amount);
        } finally {
            // Always release locks in finally block so an exception never leaves an account locked forever
            lockManager.unlockAccounts(senderId, receiverId);
        }
    }

    @Override
    public int compareTo(PaymentTask other) {
        // 1. Compare by priority level first (1 comes before 2)
        int priorityCompare = Integer.compare(this.priority, other.priority);
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        // 2. If same priority, higher transfer amount goes first
        return other.amount.compareTo(this.amount);
    }

    public String getTaskId() {
        return taskId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getPriority() {
        return priority;
    }
}