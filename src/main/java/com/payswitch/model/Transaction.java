package com.payswitch.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public class Transaction {
    public enum Status { SUCCESS, FAILED }
    private final String transactionId; private final Long senderId; private final Long receiverId; private final BigDecimal amount; private final Status status; private final String failureReason; private final LocalDateTime createdAt;
    public Transaction(String transactionId, Long senderId, Long receiverId, BigDecimal amount, Status status, String failureReason, LocalDateTime createdAt) {
        this.transactionId = transactionId; this.senderId = senderId; this.receiverId = receiverId; this.amount = amount; this.status = status; this.failureReason = failureReason; this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
    public String getTransactionId() { return transactionId; }
    public Long getSenderId() { return senderId; }
    public Long getReceiverId() { return receiverId; }
    public BigDecimal getAmount() { return amount; }
    public Status getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String toString() { return "Tx{id=" + transactionId + ", from=" + senderId + ", to=" + receiverId + ", amt=" + amount + ", status=" + status + "}"; }
}
