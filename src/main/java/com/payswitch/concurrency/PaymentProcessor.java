package com.payswitch.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Manages a fixed thread pool (ExecutorService) and a priority queue
 * to execute multiple payment tasks concurrently and safely.
 */
public class PaymentProcessor {

    private final ExecutorService executorService;
    private final PriorityQueue<PaymentTask> taskQueue;
    private final AccountLockManager lockManager;

    public PaymentProcessor(int threadPoolSize) {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        this.taskQueue = new PriorityQueue<>();
        this.lockManager = new AccountLockManager();
    }

    public AccountLockManager getLockManager() {
        return lockManager;
    }

    /**
     * Submits a single task immediately to the ExecutorService and returns a Future.
     */
    public Future<String> submitImmediate(PaymentTask task) {
        return executorService.submit(task);
    }

    /**
     * Adds a task into the PriorityQueue for batch priority execution.
     */
    public synchronized void enqueueTask(PaymentTask task) {
        taskQueue.offer(task);
    }

    /**
     * Drains all queued tasks in priority order and submits them to the thread pool.
     */
    public synchronized List<Future<String>> processQueuedBatch() {
        List<Future<String>> futures = new ArrayList<>();
        while (!taskQueue.isEmpty()) {
            PaymentTask nextHighestPriorityTask = taskQueue.poll();
            futures.add(executorService.submit(nextHighestPriorityTask));
        }
        return futures;
    }

    /**
     * Gracefully shuts down the ExecutorService after completing running tasks.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}