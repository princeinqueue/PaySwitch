package com.payswitch.model;
import java.math.BigDecimal;
public class Account {
    private final long accountId;
    private String holderName;
    private BigDecimal balance;
    public Account(long accountId, String holderName, BigDecimal initialBalance) {
        if (accountId <= 0) throw new IllegalArgumentException("Invalid account ID");
        if (holderName == null || holderName.isBlank()) throw new IllegalArgumentException("Empty holder name");
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Invalid balance");
        this.accountId = accountId; this.holderName = holderName.trim(); this.balance = initialBalance;
    }
    public long getAccountId() { return accountId; }
    public String getHolderName() { return holderName; }
    public BigDecimal getBalance() { return balance; }
    public void credit(BigDecimal amt) { if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be > 0"); balance = balance.add(amt); }
    public void debit(BigDecimal amt) { if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be > 0"); if (balance.compareTo(amt) < 0) throw new IllegalArgumentException("Insufficient balance"); balance = balance.subtract(amt); }
    public String toString() { return "Account{id=" + accountId + ", name=" + holderName + ", balance=" + balance + "}"; }
}
