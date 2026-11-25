mkdir -p database src/main/java/com/payswitch/{model,exception,service} src/test/java/com/payswitch

cat << 'EOF' > pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.payswitch</groupId>
    <artifactId>payswitch</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencies>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>9.1.0</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.11.3</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.2</version>
            </plugin>
        </plugins>
    </build>
</project>
EOF

cat << 'EOF' > .gitignore
target/
.idea/
*.iml
.DS_Store
*.log
EOF

cat << 'EOF' > src/main/java/com/payswitch/model/Account.java
package com.payswitch.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Account {
    private final long accountId;
    private String holderName;
    private BigDecimal balance;

    public Account(long accountId, String holderName, BigDecimal initialBalance) {
        if (accountId <= 0) {
            throw new IllegalArgumentException("Account ID must be positive");
        }
        if (holderName == null || holderName.isBlank()) {
            throw new IllegalArgumentException("Holder name cannot be empty");
        }
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be null or negative");
        }
        this.accountId = accountId;
        this.holderName = holderName.trim();
        this.balance = initialBalance;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        if (holderName == null || holderName.isBlank()) {
            throw new IllegalArgumentException("Holder name cannot be empty");
        }
        this.holderName = holderName.trim();
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void credit(BigDecimal amount) {
        validatePositiveAmount(amount);
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance for debit");
        }
        this.balance = this.balance.subtract(amount);
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", holderName='" + holderName + '\'' +
                ", balance=" + balance +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account other)) return false;
        return accountId == other.accountId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(accountId);
    }
}
EOF

cat << 'EOF' > src/main/java/com/payswitch/model/Transaction.java
package com.payswitch.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    public enum Status {
        SUCCESS,
        FAILED
    }

    private final String transactionId;
    private final Long senderId;
    private final Long receiverId;
    private final BigDecimal amount;
    private final Status status;
    private final String failureReason;
    private final LocalDateTime createdAt;

    public Transaction(
            String transactionId,
            Long senderId,
            Long receiverId,
            BigDecimal amount,
            Status status,
            String failureReason,
            LocalDateTime createdAt
    ) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID cannot be empty");
        }
        if (receiverId == null || receiverId <= 0) {
            throw new IllegalArgumentException("Receiver ID must be positive");
        }
        if (senderId != null && senderId <= 0) {
            throw new IllegalArgumentException("Sender ID must be positive when present");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }
        if (status == null) {
            throw new IllegalArgumentException("Transaction status cannot be null");
        }
        this.transactionId = transactionId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Status getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", amount=" + amount +
                ", status=" + status +
                (failureReason != null ? ", failureReason='" + failureReason + '\'' : "") +
                ", createdAt=" + createdAt +
                '}';
    }
}
EOF

cat << 'EOF' > src/main/java/com/payswitch/exception/InvalidAccountException.java
package com.payswitch.exception;

public class InvalidAccountException extends RuntimeException {
    public InvalidAccountException(String message) {
        super(message);
    }
}
EOF

cat << 'EOF' > src/main/java/com/payswitch/exception/InsufficientBalanceException.java
package com.payswitch.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
EOF

cat << 'EOF' > src/main/java/com/payswitch/service/PaymentService.java
package com.payswitch.service;

import com.payswitch.exception.InsufficientBalanceException;
import com.payswitch.exception.InvalidAccountException;
import com.payswitch.model.Account;
import com.payswitch.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class PaymentService {
    private final Map<Long, Account> accounts = new HashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final AtomicLong accountIdGenerator = new AtomicLong(1000);

    public synchronized Account createAccount(String holderName, BigDecimal initialBalance) {
        long newId = accountIdGenerator.incrementAndGet();
        Account account = new Account(newId, holderName, initialBalance);
        accounts.put(newId, account);
        return account;
    }

    public synchronized Account getAccount(long accountId) {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new InvalidAccountException("Account not found for ID: " + accountId);
        }
        return account;
    }

    public synchronized BigDecimal getBalance(long accountId) {
        return getAccount(accountId).getBalance();
    }

    public synchronized List<Account> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    public synchronized Transaction deposit(long accountId, BigDecimal amount) {
        validatePositiveAmount(amount);
        Account account = getAccount(accountId);
        account.credit(amount);

        Transaction tx = new Transaction(
                UUID.randomUUID().toString(),
                null,
                accountId,
                amount,
                Transaction.Status.SUCCESS,
                null,
                LocalDateTime.now()
        );
        transactions.add(tx);
        return tx;
    }

    public synchronized Transaction transfer(long senderId, long receiverId, BigDecimal amount) {
        String txId = UUID.randomUUID().toString();
        try {
            validatePositiveAmount(amount);
            if (senderId == receiverId) {
                throw new IllegalArgumentException("Sender and receiver account cannot be the same");
            }

            Account sender = getAccount(senderId);
            Account receiver = getAccount(receiverId);

            if (sender.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException(
                        "Insufficient balance in account " + senderId + ". Available: " + sender.getBalance()
                );
            }

            sender.debit(amount);
            receiver.credit(amount);

            Transaction successTx = new Transaction(
                    txId,
                    senderId,
                    receiverId,
                    amount,
                    Transaction.Status.SUCCESS,
                    null,
                    LocalDateTime.now()
            );
            transactions.add(successTx);
            return successTx;
        } catch (RuntimeException ex) {
            if (receiverId > 0 && amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                Transaction failedTx = new Transaction(
                        txId,
                        senderId > 0 ? senderId : null,
                        receiverId,
                        amount,
                        Transaction.Status.FAILED,
                        ex.getMessage(),
                        LocalDateTime.now()
                );
                transactions.add(failedTx);
            }
            throw ex;
        }
    }

    public synchronized List<Transaction> getAllTransactions() {
        return Collections.unmodifiableList(new ArrayList<>(transactions));
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
EOF

cat << 'EOF' > src/main/java/com/payswitch/Main.java
package com.payswitch;

import com.payswitch.model.Account;
import com.payswitch.model.Transaction;
import com.payswitch.service.PaymentService;

import java.math.BigDecimal;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        PaymentService service = new PaymentService();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== PAYSWITCH WALLET ENGINE ===");
            System.out.println("1. Create Account");
            System.out.println("2. Deposit Money");
            System.out.println("3. Transfer Money");
            System.out.println("4. Check Balance");
            System.out.println("5. View All Transactions");
            System.out.println("6. Exit");
            System.out.print("Enter choice: ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Enter a number 1-6.");
                continue;
            }

            try {
                switch (choice) {
                    case 1 -> {
                        System.out.print("Holder Name: ");
                        String name = scanner.nextLine();
                        System.out.print("Initial Balance: ");
                        BigDecimal bal = new BigDecimal(scanner.nextLine().trim());
                        Account acc = service.createAccount(name, bal);
                        System.out.println("Created: " + acc);
                    }
                    case 2 -> {
                        System.out.print("Account ID: ");
                        long id = Long.parseLong(scanner.nextLine().trim());
                        System.out.print("Deposit Amount: ");
                        BigDecimal amt = new BigDecimal(scanner.nextLine().trim());
                        Transaction tx = service.deposit(id, amt);
                        System.out.println("Deposited! Tx: " + tx);
                    }
                    case 3 -> {
                        System.out.print("Sender ID: ");
                        long from = Long.parseLong(scanner.nextLine().trim());
                        System.out.print("Receiver ID: ");
                        long to = Long.parseLong(scanner.nextLine().trim());
                        System.out.print("Amount: ");
                        BigDecimal amt = new BigDecimal(scanner.nextLine().trim());
                        Transaction tx = service.transfer(from, to, amt);
                        System.out.println("Transfer Success! Tx: " + tx);
                    }
                    case 4 -> {
                        System.out.print("Account ID: ");
                        long id = Long.parseLong(scanner.nextLine().trim());
                        System.out.println("Balance: " + service.getBalance(id));
                    }
                    case 5 -> {
                        for (Transaction tx : service.getAllTransactions()) {
                            System.out.println(tx);
                        }
                    }
                    case 6 -> {
                        System.out.println("Exiting PaySwitch.");
                        return;
                    }
                    default -> System.out.println("Choose between 1 and 6.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}
EOF

cat << 'EOF' > src/test/java/com/payswitch/PaymentServiceTest.java
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
