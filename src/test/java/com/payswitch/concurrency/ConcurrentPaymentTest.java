package com.payswitch.concurrency;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentPaymentTest {

    // Helper method simulating atomic balance transfer under lock
    private String executeInMemoryTransfer(ConcurrentHashMap<Long, BigDecimal> balances,
                                           long senderId, long receiverId, BigDecimal amount) {
        BigDecimal senderBal = balances.get(senderId);
        if (senderBal.compareTo(amount) < 0) {
            return "FAILED: Insufficient balance";
        }
        // Artificial 20ms delay to force threads to overlap if locking were broken
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        balances.put(senderId, senderBal.subtract(amount));
        balances.put(receiverId, balances.get(receiverId).add(amount));
        return "SUCCESS";
    }

    @Test
    void testRaceConditionPreventionOnSimultaneousWithdrawals() throws Exception {
        PaymentProcessor processor = new PaymentProcessor(4);
        ConcurrentHashMap<Long, BigDecimal> balances = new ConcurrentHashMap<>();
        balances.put(101L, new BigDecimal("1000.00"));
        balances.put(102L, new BigDecimal("0.00"));

        CountDownLatch startGate = new CountDownLatch(1);

        PaymentTask task1 = new PaymentTask("TX-1", 101L, 102L, new BigDecimal("800.00"), 1,
                processor.getLockManager(), (s, r, amt) -> {
            startGate.await();
            return executeInMemoryTransfer(balances, s, r, amt);
        });

        PaymentTask task2 = new PaymentTask("TX-2", 101L, 102L, new BigDecimal("800.00"), 1,
                processor.getLockManager(), (s, r, amt) -> {
            startGate.await();
            return executeInMemoryTransfer(balances, s, r, amt);
        });

        Future<String> f1 = processor.submitImmediate(task1);
        Future<String> f2 = processor.submitImmediate(task2);

        // Release both threads at the exact same instant
        startGate.countDown();

        String res1 = f1.get();
        String res2 = f2.get();
        processor.shutdown();

        System.out.println("Race Test -> Task 1: " + res1 + " | Task 2: " + res2);
        System.out.println("Final Account 101 Balance: " + balances.get(101L));

        assertEquals(new BigDecimal("200.00"), balances.get(101L));
        assertEquals(new BigDecimal("800.00"), balances.get(102L));
        assertTrue((res1.equals("SUCCESS") && res2.startsWith("FAILED")) ||
                   (res2.equals("SUCCESS") && res1.startsWith("FAILED")));
    }

    @Test
    void testDeadlockPreventionOnOppositeDirectionTransfers() throws Exception {
        PaymentProcessor processor = new PaymentProcessor(8);
        ConcurrentHashMap<Long, BigDecimal> balances = new ConcurrentHashMap<>();
        balances.put(101L, new BigDecimal("5000.00"));
        balances.put(102L, new BigDecimal("5000.00"));

        List<Future<String>> futures = new ArrayList<>();

        // Submit 20 simultaneous opposite-direction transfers (101 <-> 102)
        for (int i = 0; i < 20; i++) {
            long sender = (i % 2 == 0) ? 101L : 102L;
            long receiver = (i % 2 == 0) ? 102L : 101L;

            PaymentTask task = new PaymentTask("TX-" + i, sender, receiver, new BigDecimal("100.00"), 1,
                    processor.getLockManager(),
                    (s, r, amt) -> executeInMemoryTransfer(balances, s, r, amt));

            futures.add(processor.submitImmediate(task));
        }

        for (Future<String> f : futures) {
            assertEquals("SUCCESS", f.get());
        }
        processor.shutdown();

        // Conservation of money check: total across both accounts must stay 10000.00
        BigDecimal totalMoney = balances.get(101L).add(balances.get(102L));
        System.out.println("Opposite-Direction Test -> Total Money Preserved: " + totalMoney);
        assertEquals(new BigDecimal("10000.00"), totalMoney);
    }

    @Test
    void testPriorityQueueOrdering() throws Exception {
        PaymentProcessor processor = new PaymentProcessor(1); // 1 worker thread to observe strict order
        List<String> executionOrder = new ArrayList<>();

        PaymentTask lowPriority = new PaymentTask("LOW-1", 101L, 102L, new BigDecimal("500.00"), 2,
                processor.getLockManager(), (s, r, amt) -> {
            executionOrder.add("LOW-1");
            return "DONE";
        });

        PaymentTask highPrioritySmall = new PaymentTask("HIGH-SMALL", 101L, 102L, new BigDecimal("100.00"), 1,
                processor.getLockManager(), (s, r, amt) -> {
            executionOrder.add("HIGH-SMALL");
            return "DONE";
        });

        PaymentTask highPriorityLarge = new PaymentTask("HIGH-LARGE", 101L, 102L, new BigDecimal("2000.00"), 1,
                processor.getLockManager(), (s, r, amt) -> {
            executionOrder.add("HIGH-LARGE");
            return "DONE";
        });

        processor.enqueueTask(lowPriority);
        processor.enqueueTask(highPrioritySmall);
        processor.enqueueTask(highPriorityLarge);

        for (Future<String> f : processor.processQueuedBatch()) {
            f.get();
        }
        processor.shutdown();

        System.out.println("Priority Execution Order: " + executionOrder);
        assertEquals(List.of("HIGH-LARGE", "HIGH-SMALL", "LOW-1"), executionOrder);
    }
}
