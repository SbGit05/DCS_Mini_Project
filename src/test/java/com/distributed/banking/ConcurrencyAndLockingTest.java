package com.distributed.banking;

import com.distributed.banking.model.Account;
import com.distributed.banking.model.Branch;
import com.distributed.banking.model.User;
import com.distributed.banking.repository.AccountRepository;
import com.distributed.banking.repository.BranchRepository;
import com.distributed.banking.repository.UserRepository;
import com.distributed.banking.service.AccountService;
import com.distributed.banking.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ConcurrencyAndLockingTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    @DisplayName("Scenario 8: Multi-threaded Simultaneous Withdrawals Concurrency Test")
    void testConcurrentSimultaneousWithdrawals() throws InterruptedException {
        // Create an isolated account with exactly 5000.00
        String accNum = "CONCUR-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Branch branch = branchRepository.findById(1L).orElseThrow();
        User user = userRepository.findById(2L).orElseThrow();

        Account account = new Account(accNum, user, branch, "SAVINGS", new BigDecimal("5000.00"), "ACTIVE");
        accountRepository.save(account);

        int totalThreads = 10;
        BigDecimal withdrawAmount = new BigDecimal("1000.00");

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready to strike simultaneously
                    transactionService.withdraw(accNum, withdrawAmount, "IDEM-CONCUR-" + index + "-" + UUID.randomUUID());
                    successCount.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    // Insufficient funds caught safely
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all 10 threads concurrently
        startLatch.countDown();
        finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        BigDecimal finalBalance = accountService.getBalance(accNum);

        // Verification of atomicity and row-level locking:
        // Exactly 5 withdrawals of 1000 could possibly succeed from a 5000 balance!
        assertEquals(5, successCount.get(), "Exactly 5 withdrawals should succeed");
        assertEquals(5, failCount.get(), "Exactly 5 withdrawals should fail due to insufficient funds");
        assertEquals(new BigDecimal("0.00"), finalBalance, "Final balance must be exactly 0.00 without any double-spending");
    }
}
