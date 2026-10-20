package com.distributed.banking.service;

import com.distributed.banking.distributed.LamportClock;
import com.distributed.banking.distributed.ReplicationManager;
import com.distributed.banking.distributed.TwoPhaseCommitCoordinator;
import com.distributed.banking.model.Account;
import com.distributed.banking.model.LamportEvent;
import com.distributed.banking.model.Transaction;
import com.distributed.banking.model.TwoPhaseTransaction;
import com.distributed.banking.repository.AccountRepository;
import com.distributed.banking.repository.LamportEventRepository;
import com.distributed.banking.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LamportEventRepository eventRepository;
    private final LamportClock lamportClock;
    private final ReplicationManager replicationManager;
    private final TwoPhaseCommitCoordinator coordinator;
    private final TransactionTemplate transactionTemplate;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              LamportEventRepository eventRepository,
                              LamportClock lamportClock,
                              ReplicationManager replicationManager,
                              TwoPhaseCommitCoordinator coordinator,
                              PlatformTransactionManager transactionManager) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.eventRepository = eventRepository;
        this.lamportClock = lamportClock;
        this.replicationManager = replicationManager;
        this.coordinator = coordinator;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Deposit operation using pessimistic row-level locking and idempotency protection.
     */
    public Transaction deposit(String accountNumber, BigDecimal amount, String idempotencyKey) {
        validateAmount(amount);

        // Check idempotency
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent deposit request returning existing transaction: {}", existing.get().getTransactionRef());
                return existing.get();
            }
        }

        return transactionTemplate.execute(status -> {
            // Row-level lock
            Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));

            if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
                throw new IllegalStateException("Account " + accountNumber + " is " + account.getStatus());
            }

            account.setBalance(account.getBalance().add(amount));
            accountRepository.save(account);

            long ts = lamportClock.tick();
            String ref = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Transaction txn = new Transaction(
                    ref, idempotencyKey, null, accountNumber, "DEPOSIT",
                    amount, account.getBranch().getId(), null, "SUCCESS", null, ts
            );
            txn = transactionRepository.save(txn);

            logLamportEvent(account.getBranch().getId(), account.getBranch().getBranchName(), ts,
                    "TXN_DEPOSIT", "Deposited " + amount + " to account " + accountNumber + " (New balance: " + account.getBalance() + ")");

            // Replicate to peer branch audit log
            replicationManager.recordTransaction(account.getBranch().getId(), 0L, ref, accountNumber, "BALANCE_UPDATE", amount);

            return txn;
        });
    }

    /**
     * Concurrency-safe withdrawal using pessimistic row locking to prevent race conditions.
     */
    public Transaction withdraw(String accountNumber, BigDecimal amount, String idempotencyKey) {
        validateAmount(amount);

        // Check idempotency
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent withdrawal request returning existing transaction: {}", existing.get().getTransactionRef());
                return existing.get();
            }
        }

        return transactionTemplate.execute(status -> {
            // Critical: Exclusive row-level lock (SELECT ... FOR UPDATE)
            Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));

            if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
                throw new IllegalStateException("Account " + accountNumber + " is " + account.getStatus());
            }

            if (account.getBalance().compareTo(amount) < 0) {
                long ts = lamportClock.tick();
                String ref = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                Transaction failedTxn = new Transaction(
                        ref, idempotencyKey, accountNumber, null, "WITHDRAWAL",
                        amount, account.getBranch().getId(), null, "FAILED",
                        "Insufficient funds (Current Balance: " + account.getBalance() + ")", ts
                );
                transactionRepository.save(failedTxn);

                logLamportEvent(account.getBranch().getId(), account.getBranch().getBranchName(), ts,
                        "TXN_WITHDRAW_REJECT", "Insufficient funds for withdrawal of " + amount + " from " + accountNumber);

                throw new IllegalArgumentException("Insufficient funds. Available balance: " + account.getBalance());
            }

            account.setBalance(account.getBalance().subtract(amount));
            accountRepository.save(account);

            long ts = lamportClock.tick();
            String ref = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Transaction txn = new Transaction(
                    ref, idempotencyKey, accountNumber, null, "WITHDRAWAL",
                    amount, account.getBranch().getId(), null, "SUCCESS", null, ts
            );
            txn = transactionRepository.save(txn);

            logLamportEvent(account.getBranch().getId(), account.getBranch().getBranchName(), ts,
                    "TXN_WITHDRAW", "Withdrew " + amount + " from account " + accountNumber + " (Remaining: " + account.getBalance() + ")");

            // Replicate to peer branch audit log
            replicationManager.recordTransaction(account.getBranch().getId(), 0L, ref, accountNumber, "BALANCE_UPDATE", amount.negate());

            return txn;
        });
    }

    /**
     * Unified Fund Transfer:
     * - Same Branch: Atomic local transaction with deterministic lock ordering to prevent deadlocks.
     * - Different Branch: Distributed Two-Phase Commit (2PC) over RMI.
     */
    public Transaction transfer(String fromAccount, String toAccount, BigDecimal amount, String idempotencyKey) {
        validateAmount(amount);
        if (fromAccount.equalsIgnoreCase(toAccount)) {
            throw new IllegalArgumentException("Source and destination accounts cannot be identical.");
        }

        Account source = accountRepository.findByAccountNumber(fromAccount)
                .orElseThrow(() -> new IllegalArgumentException("Source account " + fromAccount + " not found."));
        Account dest = accountRepository.findByAccountNumber(toAccount)
                .orElseThrow(() -> new IllegalArgumentException("Destination account " + toAccount + " not found."));

        Long sourceBranchId = source.getBranch().getId();
        Long destBranchId = dest.getBranch().getId();

        if (sourceBranchId.equals(destBranchId)) {
            // Local transfer within same branch
            return executeLocalTransfer(fromAccount, toAccount, amount, idempotencyKey, sourceBranchId);
        } else {
            // Cross-branch distributed transfer -> Two-Phase Commit
            log.info("Cross-branch transfer detected (Branch {} -> Branch {}). Delegating to 2PC Coordinator.",
                    sourceBranchId, destBranchId);

            TwoPhaseTransaction gtx = coordinator.executeTransfer(
                    fromAccount, toAccount, sourceBranchId, destBranchId, amount, idempotencyKey
            );

            // Fetch resulting authoritative transaction
            return transactionRepository.findByTransactionRef(gtx.getGlobalTransactionId())
                    .orElseThrow(() -> new RuntimeException("Transaction state unavailable for GTX: " + gtx.getGlobalTransactionId()));
        }
    }

    /**
     * Local Transfer with consistent lock ordering to prevent deadlocks between concurrent transfers.
     */
    public Transaction executeLocalTransfer(String fromAccount, String toAccount, BigDecimal amount,
                                            String idempotencyKey, Long branchId) {
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        return transactionTemplate.execute(status -> {
            // Consistent lock ordering (lexicographical) to guarantee no deadlock cycles
            String firstLockAcc = fromAccount.compareTo(toAccount) < 0 ? fromAccount : toAccount;
            String secondLockAcc = fromAccount.compareTo(toAccount) < 0 ? toAccount : fromAccount;

            Account first = accountRepository.findByAccountNumberForUpdate(firstLockAcc).orElseThrow();
            Account second = accountRepository.findByAccountNumberForUpdate(secondLockAcc).orElseThrow();

            Account src = fromAccount.equals(first.getAccountNumber()) ? first : second;
            Account dst = toAccount.equals(first.getAccountNumber()) ? first : second;

            if (src.getBalance().compareTo(amount) < 0) {
                throw new IllegalArgumentException("Insufficient balance in source account: " + src.getBalance());
            }

            src.setBalance(src.getBalance().subtract(amount));
            dst.setBalance(dst.getBalance().add(amount));

            accountRepository.save(src);
            accountRepository.save(dst);

            long ts = lamportClock.tick();
            String ref = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Transaction txn = new Transaction(
                    ref, idempotencyKey, fromAccount, toAccount, "TRANSFER_LOCAL",
                    amount, branchId, branchId, "SUCCESS", null, ts
            );
            txn = transactionRepository.save(txn);

            logLamportEvent(branchId, src.getBranch().getBranchName(), ts,
                    "TXN_LOCAL_TRANSFER", "Transferred " + amount + " from " + fromAccount + " to " + toAccount);

            replicationManager.recordTransaction(branchId, branchId, ref, fromAccount, "BALANCE_UPDATE", amount);

            return txn;
        });
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getAllRecentTransactions() {
        return transactionRepository.findTop50ByOrderByCreatedAtDesc();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive and greater than zero.");
        }
    }

    private void logLamportEvent(Long nodeId, String nodeName, long ts, String eventType, String msg) {
        try {
            LamportEvent event = new LamportEvent(nodeId, nodeName, ts, eventType, msg);
            eventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to log Lamport event: {}", e.getMessage());
        }
    }
}
