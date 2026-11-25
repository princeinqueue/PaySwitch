package com.payswitch.service;
import com.payswitch.exception.*;
import com.payswitch.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
public class PaymentService {
    private final Map<Long, Account> accounts = new HashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final AtomicLong idGen = new AtomicLong(1000);
    public synchronized Account createAccount(String name, BigDecimal initBal) { long id = idGen.incrementAndGet(); Account a = new Account(id, name, initBal); accounts.put(id, a); return a; }
    public synchronized Account getAccount(long id) { Account a = accounts.get(id); if (a == null) throw new InvalidAccountException("Invalid account: " + id); return a; }
    public synchronized BigDecimal getBalance(long id) { return getAccount(id).getBalance(); }
    public synchronized Transaction deposit(long id, BigDecimal amt) { checkAmt(amt); Account a = getAccount(id); a.credit(amt); Transaction tx = new Transaction(UUID.randomUUID().toString(), null, id, amt, Transaction.Status.SUCCESS, null, LocalDateTime.now()); transactions.add(tx); return tx; }
    public synchronized Transaction transfer(long fromId, long toId, BigDecimal amt) { String txId = UUID.randomUUID().toString(); try { checkAmt(amt); if (fromId == toId) throw new IllegalArgumentException("Same account transfer not allowed"); Account s = getAccount(fromId); Account r = getAccount(toId); if (s.getBalance().compareTo(amt) < 0) throw new InsufficientBalanceException("Insufficient balance"); s.debit(amt); r.credit(amt); Transaction tx = new Transaction(txId, fromId, toId, amt, Transaction.Status.SUCCESS, null, LocalDateTime.now()); transactions.add(tx); return tx; } catch (RuntimeException ex) { transactions.add(new Transaction(txId, fromId, toId, amt, Transaction.Status.FAILED, ex.getMessage(), LocalDateTime.now())); throw ex; } }
    public synchronized List<Transaction> getAllTransactions() { return Collections.unmodifiableList(transactions); }
    private void checkAmt(BigDecimal amt) { if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be > 0"); }
}
