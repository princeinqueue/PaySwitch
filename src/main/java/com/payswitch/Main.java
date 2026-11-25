package com.payswitch;
import com.payswitch.model.*;
import com.payswitch.service.PaymentService;
import java.math.BigDecimal;
public class Main {
    public static void main(String[] args) {
        PaymentService ps = new PaymentService();
        Account a1 = ps.createAccount("Mehul", new BigDecimal("5000.00"));
        Account a2 = ps.createAccount("Keshav", new BigDecimal("2000.00"));
        ps.deposit(a1.getAccountId(), new BigDecimal("1000.00"));
        ps.transfer(a1.getAccountId(), a2.getAccountId(), new BigDecimal("1500.00"));
        System.out.println(a1); System.out.println(a2);
        ps.getAllTransactions().forEach(System.out::println);
    }
}
