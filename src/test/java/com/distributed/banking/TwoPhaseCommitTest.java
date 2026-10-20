package com.distributed.banking;

import com.distributed.banking.distributed.TwoPhaseCommitCoordinator;
import com.distributed.banking.model.Transaction;
import com.distributed.banking.model.TwoPhaseTransaction;
import com.distributed.banking.repository.TwoPhaseTransactionRepository;
import com.distributed.banking.service.AccountService;
import com.distributed.banking.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TwoPhaseCommitTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TwoPhaseTransactionRepository twoPhaseRepository;

    @Autowired
    private TwoPhaseCommitCoordinator coordinator;

    @Test
    @DisplayName("Scenario 16: Two-Phase Commit (2PC) Successful Distributed Transfer Path")
    void testTwoPhaseCommitSuccess() {
        String fromAcc = "PUN-1001"; // Pune Central (Branch 1)
        String toAcc = "MUM-2001";   // Mumbai Main (Branch 2)

        BigDecimal initialFrom = accountService.getBalance(fromAcc);
        BigDecimal initialTo = accountService.getBalance(toAcc);

        BigDecimal transferAmount = new BigDecimal("2000.00");
        String idempotencyKey = "2PC-SUCCESS-" + UUID.randomUUID();

        Transaction txn = transactionService.transfer(fromAcc, toAcc, transferAmount, idempotencyKey);

        assertEquals("SUCCESS", txn.getStatus());
        assertEquals("TRANSFER_DISTRIBUTED", txn.getTransactionType());

        // Verify balance changes across branches
        BigDecimal finalFrom = accountService.getBalance(fromAcc);
        BigDecimal finalTo = accountService.getBalance(toAcc);

        assertEquals(initialFrom.subtract(transferAmount), finalFrom);
        assertEquals(initialTo.add(transferAmount), finalTo);

        // Verify 2PC transaction coordinator record
        TwoPhaseTransaction gtx = twoPhaseRepository.findByGlobalTransactionId(txn.getTransactionRef()).orElseThrow();
        assertEquals("VOTE_COMMIT", gtx.getPhase1SourceVote());
        assertEquals("VOTE_COMMIT", gtx.getPhase1DestVote());
        assertEquals("GLOBAL_COMMIT", gtx.getDecision());
        assertEquals("COMMITTED", gtx.getState());
        assertTrue(gtx.getLamportTimestamp() > 0);
    }

    @Test
    @DisplayName("Scenario 17: Two-Phase Commit (2PC) Abort Path on Insufficient Balance")
    void testTwoPhaseCommitAbort() {
        String fromAcc = "PUN-1002"; // Pune Central (Branch 1)
        String toAcc = "MUM-2002";   // Mumbai Main (Branch 2)

        BigDecimal initialFrom = accountService.getBalance(fromAcc);
        BigDecimal initialTo = accountService.getBalance(toAcc);

        BigDecimal excessiveAmount = new BigDecimal("9999999.00");
        String idempotencyKey = "2PC-ABORT-" + UUID.randomUUID();

        Transaction txn = transactionService.transfer(fromAcc, toAcc, excessiveAmount, idempotencyKey);

        assertEquals("ABORTED", txn.getStatus());
        assertNotNull(txn.getFailureReason());

        // Balances must remain strictly unchanged across both branches
        assertEquals(initialFrom, accountService.getBalance(fromAcc));
        assertEquals(initialTo, accountService.getBalance(toAcc));

        // Verify 2PC transaction abort record
        TwoPhaseTransaction gtx = twoPhaseRepository.findByGlobalTransactionId(txn.getTransactionRef()).orElseThrow();
        assertEquals("VOTE_ABORT", gtx.getPhase1SourceVote());
        assertEquals("GLOBAL_ABORT", gtx.getDecision());
        assertEquals("ABORTED", gtx.getState());
    }
}
