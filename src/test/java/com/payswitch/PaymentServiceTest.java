package com.payswitch;

import com.payswitch.exception.InsufficientBalanceException;
import com.payswitch.exception.InvalidAccountException;
import com.payswitch.model.Account;
import com.payswitch.model.Transaction;
import com.payswitch.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {

    private PaymentService paymentService;
    private Account mehul;
    private Account keshav;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
        mehul = paymentService.createAccount("Mehul", new BigDecimal("5000.00"));
        keshav = paymentService.createAccount("Keshav", new BigDecimal("2000.00"));
    }

    @Test
    void testCreateAccountAndDeposit() {
        Transaction tx = paymentService.deposit(mehul.getAccountId(), new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("6000.00"), paymentService.getBalance(mehul.getAccountId()));
        assertEquals(Transaction.Status.SUCCESS, tx.getStatus());
        assertNull(tx.getSenderId());
    }

    @Test
    void testSuccessfulTransfer() {
        Transaction tx = paymentService.transfer(
                mehul.getAccountId(),
                keshav.getAccountId(),
                new BigDecimal("1500.00")
        );

        assertEquals(new BigDecimal("3500.00"), paymentService.getBalance(mehul.getAccountId()));
        assertEquals(new BigDecimal("3500.00"), paymentService.getBalance(keshav.getAccountId()));
        assertEquals(Transaction.Status.SUCCESS, tx.getStatus());
    }

    @Test
    void testInsufficientBalanceThrowsAndLeavesBalancesUnchanged() {
        assertThrows(InsufficientBalanceException.class, () ->
                paymentService.transfer(
                        keshav.getAccountId(),
                        mehul.getAccountId(),
                        new BigDecimal("3000.00")
                )
        );

        assertEquals(new BigDecimal("5000.00"), paymentService.getBalance(mehul.getAccountId()));
        assertEquals(new BigDecimal("2000.00"), paymentService.getBalance(keshav.getAccountId()));
        assertEquals(1, paymentService.getAllTransactions().size());
        assertEquals(Transaction.Status.FAILED, paymentService.getAllTransactions().get(0).getStatus());
    }

    @Test
    void testInvalidAccountThrows() {
        assertThrows(InvalidAccountException.class, () ->
                paymentService.transfer(mehul.getAccountId(), 99999L, new BigDecimal("500.00"))
        );
        assertEquals(new BigDecimal("5000.00"), paymentService.getBalance(mehul.getAccountId()));
    }

    @Test
    void testSameAccountAndNegativeAmountRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.transfer(mehul.getAccountId(), mehul.getAccountId(), new BigDecimal("100.00"))
        );
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.deposit(mehul.getAccountId(), new BigDecimal("-50.00"))
        );
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.transfer(mehul.getAccountId(), keshav.getAccountId(), BigDecimal.ZERO)
        );
    }
}
