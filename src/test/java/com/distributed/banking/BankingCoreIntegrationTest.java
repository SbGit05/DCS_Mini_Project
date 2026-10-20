package com.distributed.banking;

import com.distributed.banking.model.Account;
import com.distributed.banking.model.Transaction;
import com.distributed.banking.model.User;
import com.distributed.banking.repository.AccountRepository;
import com.distributed.banking.repository.UserRepository;
import com.distributed.banking.service.AccountService;
import com.distributed.banking.service.AuthService;
import com.distributed.banking.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BankingCoreIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Scenario 1: Customer Registration and Authentication")
    void testRegistrationAndLogin() {
        String uniqueUser = "testuser_" + UUID.randomUUID().toString().substring(0, 6);
        User user = authService.registerCustomer(uniqueUser, "pass1234", "Test Client", uniqueUser + "@test.internal", 1L);

        assertNotNull(user.getId(), "Registered user must have generated ID");
        assertEquals(uniqueUser, user.getUsername());
        assertEquals("ROLE_CUSTOMER", user.getRole());

        // Test login
        Map<String, Object> session = authService.login(uniqueUser, "pass1234");
        assertNotNull(session.get("token"));
        assertEquals(uniqueUser, session.get("username"));

        // Validate session token
        User validatedUser = authService.validateToken((String) session.get("token"));
        assertEquals(user.getId(), validatedUser.getId());

        // Test invalid login password
        assertThrows(IllegalArgumentException.class, () -> authService.login(uniqueUser, "wrongpassword"));
    }

    @Test
    @DisplayName("Scenario 2 & 3: Account Deposit and Withdrawal with Balance Assertion")
    void testDepositAndWithdrawal() {
        String testAcc = "PUN-1001";
        BigDecimal initialBalance = accountService.getBalance(testAcc);

        BigDecimal depositAmount = new BigDecimal("2500.00");
        Transaction depTxn = transactionService.deposit(testAcc, depositAmount, "IDEM-DEP-" + UUID.randomUUID());
        assertEquals("SUCCESS", depTxn.getStatus());
        assertEquals("DEPOSIT", depTxn.getTransactionType());

        BigDecimal balanceAfterDep = accountService.getBalance(testAcc);
        assertEquals(initialBalance.add(depositAmount), balanceAfterDep);

        BigDecimal withdrawAmount = new BigDecimal("1000.00");
        Transaction wthTxn = transactionService.withdraw(testAcc, withdrawAmount, "IDEM-WTH-" + UUID.randomUUID());
        assertEquals("SUCCESS", wthTxn.getStatus());
        assertEquals("WITHDRAWAL", wthTxn.getTransactionType());

        BigDecimal balanceAfterWth = accountService.getBalance(testAcc);
        assertEquals(balanceAfterDep.subtract(withdrawAmount), balanceAfterWth);
    }

    @Test
    @DisplayName("Scenario 5: Withdrawal Rejection on Insufficient Funds")
    void testWithdrawalInsufficientFunds() {
        String testAcc = "PUN-1002";
        BigDecimal currentBalance = accountService.getBalance(testAcc);
        BigDecimal overdraftAmount = currentBalance.add(new BigDecimal("50000.00"));

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.withdraw(testAcc, overdraftAmount, "IDEM-OVERDRAFT-" + UUID.randomUUID())
        );

        // Balance must remain strictly unchanged
        assertEquals(currentBalance, accountService.getBalance(testAcc));
    }

    @Test
    @DisplayName("Scenario 6: Invalid Amount Rejection (Negative and Zero)")
    void testInvalidAmountRejection() {
        String testAcc = "PUN-1001";

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.deposit(testAcc, new BigDecimal("-100.00"), null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.withdraw(testAcc, BigDecimal.ZERO, null)
        );
    }

    @Test
    @DisplayName("Scenario 4: Local Transfer Between Same Branch Accounts")
    void testLocalTransfer() {
        String fromAcc = "PUN-1001";
        String toAcc = "PUN-1002";

        BigDecimal initialFrom = accountService.getBalance(fromAcc);
        BigDecimal initialTo = accountService.getBalance(toAcc);

        BigDecimal transferAmount = new BigDecimal("1500.00");
        Transaction txn = transactionService.transfer(fromAcc, toAcc, transferAmount, "IDEM-LOCAL-" + UUID.randomUUID());

        assertEquals("SUCCESS", txn.getStatus());
        assertEquals("TRANSFER_LOCAL", txn.getTransactionType());

        assertEquals(initialFrom.subtract(transferAmount), accountService.getBalance(fromAcc));
        assertEquals(initialTo.add(transferAmount), accountService.getBalance(toAcc));
    }

    @Test
    @DisplayName("Scenario 9: Idempotency Protection Against Duplicate Transactions")
    void testDuplicateTransactionHandling() {
        String testAcc = "PUN-1001";
        String idempotencyKey = "UNIQUE-IDEMP-" + UUID.randomUUID();

        Transaction firstTxn = transactionService.deposit(testAcc, new BigDecimal("500.00"), idempotencyKey);
        assertNotNull(firstTxn.getTransactionRef());

        BigDecimal balanceAfterFirst = accountService.getBalance(testAcc);

        // Retry same operation with exact same idempotency key
        Transaction secondTxn = transactionService.deposit(testAcc, new BigDecimal("500.00"), idempotencyKey);

        assertEquals(firstTxn.getTransactionRef(), secondTxn.getTransactionRef(), "Duplicate request must return original transaction ref");
        assertEquals(balanceAfterFirst, accountService.getBalance(testAcc), "Balance must not be double-credited on retry");
    }
}
