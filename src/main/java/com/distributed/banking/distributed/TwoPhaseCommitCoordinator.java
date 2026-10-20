package com.distributed.banking.distributed;

import com.distributed.banking.model.Transaction;
import com.distributed.banking.model.TwoPhaseTransaction;
import com.distributed.banking.repository.BranchRepository;
import com.distributed.banking.repository.LamportEventRepository;
import com.distributed.banking.repository.TransactionRepository;
import com.distributed.banking.repository.TwoPhaseTransactionRepository;
import com.distributed.banking.rmi.BranchServiceRemote;
import com.distributed.banking.rmi.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Two-Phase Commit (2PC) Distributed Transaction Coordinator.
 * Manages the atomic execution of cross-branch fund transfers across
 * independent RMI branch services.
 */
@Component
public class TwoPhaseCommitCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TwoPhaseCommitCoordinator.class);

    private final LamportClock lamportClock;
    private final TwoPhaseTransactionRepository twoPhaseRepository;
    private final TransactionRepository transactionRepository;
    private final BranchRepository branchRepository;
    private final LamportEventRepository eventRepository;
    private final ReplicationManager replicationManager;

    private final Map<Long, BranchServiceRemote> branchStubs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private volatile Long activeCoordinatorId = 3L; // Node 3 is initial coordinator
    private volatile boolean simulatedFailure = false;

    public TwoPhaseCommitCoordinator(LamportClock lamportClock,
                                     TwoPhaseTransactionRepository twoPhaseRepository,
                                     TransactionRepository transactionRepository,
                                     BranchRepository branchRepository,
                                     LamportEventRepository eventRepository,
                                     ReplicationManager replicationManager) {
        this.lamportClock = lamportClock;
        this.twoPhaseRepository = twoPhaseRepository;
        this.transactionRepository = transactionRepository;
        this.branchRepository = branchRepository;
        this.eventRepository = eventRepository;
        this.replicationManager = replicationManager;
    }

    public void registerBranchStub(Long nodeId, BranchServiceRemote stub) {
        branchStubs.put(nodeId, stub);
    }

    public BranchServiceRemote getBranchStub(Long nodeId) {
        BranchServiceRemote stub = branchStubs.get(nodeId);
        if (stub != null) {
            return stub;
        }
        // Try resolving via RMI registry
        try {
            var branchOpt = branchRepository.findById(nodeId);
            if (branchOpt.isPresent()) {
                var b = branchOpt.get();
                Registry reg = LocateRegistry.getRegistry(b.getRmiHost(), b.getRmiPort());
                stub = (BranchServiceRemote) reg.lookup(b.getRmiServiceName());
                branchStubs.put(nodeId, stub);
                return stub;
            }
        } catch (Exception e) {
            log.warn("Could not dynamically resolve RMI stub for node {}: {}", nodeId, e.getMessage());
        }
        return null;
    }

    /**
     * Executes cross-branch transfer using 2PC protocol.
     */
    public TwoPhaseTransaction executeTransfer(String fromAccount, String toAccount,
                                              Long sourceBranchId, Long destBranchId,
                                              BigDecimal amount, String idempotencyKey) {
        if (simulatedFailure) {
            throw new RuntimeException("2PC Coordinator (Node " + activeCoordinatorId + ") is UNAVAILABLE (Simulated Failure).");
        }

        // 1. Idempotency check: Return existing committed/aborted transaction if already processed
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<Transaction> existingTxn = transactionRepository.findByIdempotencyKey(idempotencyKey);
            if (existingTxn.isPresent()) {
                Transaction t = existingTxn.get();
                Optional<TwoPhaseTransaction> existingGtx = twoPhaseRepository.findByGlobalTransactionId(t.getTransactionRef());
                if (existingGtx.isPresent()) {
                    log.info("Idempotent request returning existing 2PC transaction: {}", t.getTransactionRef());
                    return existingGtx.get();
                }
            }
        }

        String gtxId = "GTX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long currentTs = lamportClock.tick();

        log.info("[2PC-START] Initiating 2PC Global Transaction: {}, From: {} (Branch {}), To: {} (Branch {}), Amount: {}, L(e)={}",
                gtxId, fromAccount, sourceBranchId, toAccount, destBranchId, amount, currentTs);

        TwoPhaseTransaction gtx = new TwoPhaseTransaction(
                gtxId, fromAccount, toAccount, amount, sourceBranchId, destBranchId, activeCoordinatorId, "INITIATED", currentTs
        );
        gtx = twoPhaseRepository.save(gtx);

        BranchServiceRemote sourceStub = getBranchStub(sourceBranchId);
        BranchServiceRemote destStub = getBranchStub(destBranchId);

        if (sourceStub == null || destStub == null) {
            String abortReason = (sourceStub == null ? "Source Branch " + sourceBranchId : "Destination Branch " + destBranchId) + " unreachable";
            return finalizeAbort(gtx, "VOTE_ABORT", "VOTE_ABORT", abortReason, idempotencyKey);
        }

        // ==========================================
        // PHASE 1: PREPARE
        // ==========================================
        PrepareResponse sourceVote;
        PrepareResponse destVote;

        try {
            Future<PrepareResponse> sourceFuture = executor.submit(() ->
                    sourceStub.prepareTransaction(new PrepareRequest(gtxId, fromAccount, amount, true, lamportClock.tick()))
            );
            Future<PrepareResponse> destFuture = executor.submit(() ->
                    destStub.prepareTransaction(new PrepareRequest(gtxId, toAccount, amount, false, lamportClock.tick()))
            );

            sourceVote = sourceFuture.get(4, TimeUnit.SECONDS);
            destVote = destFuture.get(4, TimeUnit.SECONDS);

            gtx.setPhase1SourceVote(sourceVote.getVote());
            gtx.setPhase1DestVote(destVote.getVote());

        } catch (TimeoutException te) {
            log.error("[2PC-PHASE1] Timeout during prepare phase for GTX {}", gtxId);
            return finalizeAbort(gtx, "TIMEOUT", "TIMEOUT", "Participant vote timed out", idempotencyKey);
        } catch (Exception e) {
            log.error("[2PC-PHASE1] Exception during prepare phase for GTX {}: {}", gtxId, e.getMessage());
            return finalizeAbort(gtx, "VOTE_ABORT", "VOTE_ABORT", "Participant prepare failure: " + e.getMessage(), idempotencyKey);
        }

        // ==========================================
        // PHASE 2: COMMIT OR ABORT DECISION
        // ==========================================
        boolean canCommit = "VOTE_COMMIT".equalsIgnoreCase(sourceVote.getVote()) &&
                            "VOTE_COMMIT".equalsIgnoreCase(destVote.getVote());

        if (canCommit) {
            return finalizeCommit(gtx, sourceStub, destStub, idempotencyKey);
        } else {
            String reason = !"VOTE_COMMIT".equalsIgnoreCase(sourceVote.getVote()) ? sourceVote.getReason() : destVote.getReason();
            return finalizeAbort(gtx, sourceVote.getVote(), destVote.getVote(), reason, idempotencyKey);
        }
    }

    private TwoPhaseTransaction finalizeCommit(TwoPhaseTransaction gtx, BranchServiceRemote sourceStub,
                                                BranchServiceRemote destStub, String idempotencyKey) {
        log.info("[2PC-PHASE2] Both participants voted COMMIT for GTX {}. Broadcasting GLOBAL_COMMIT.", gtx.getGlobalTransactionId());

        gtx.setDecision("GLOBAL_COMMIT");
        gtx.setState("COMMITTED");
        gtx.setLamportTimestamp(lamportClock.tick());
        twoPhaseRepository.save(gtx);

        // Notify participants to commit
        try {
            sourceStub.commitTransaction(new CommitRequest(gtx.getGlobalTransactionId(), "GLOBAL_COMMIT", lamportClock.tick()));
            destStub.commitTransaction(new CommitRequest(gtx.getGlobalTransactionId(), "GLOBAL_COMMIT", lamportClock.tick()));
        } catch (Exception e) {
            log.error("[2PC-PHASE2-WARN] Error notifying participant of commit: {}", e.getMessage());
        }

        // Persist authoritative transaction record
        Transaction txn = new Transaction(
                gtx.getGlobalTransactionId(),
                idempotencyKey,
                gtx.getSourceAccount(),
                gtx.getDestAccount(),
                "TRANSFER_DISTRIBUTED",
                gtx.getAmount(),
                gtx.getSourceBranchId(),
                gtx.getDestBranchId(),
                "SUCCESS",
                null,
                lamportClock.tick()
        );
        transactionRepository.save(txn);

        // Replicate to peer branch audit log
        replicationManager.recordTransaction(gtx.getSourceBranchId(), gtx.getDestBranchId(),
                gtx.getGlobalTransactionId(), gtx.getSourceAccount(), "BALANCE_UPDATE", gtx.getAmount());

        return gtx;
    }

    private TwoPhaseTransaction finalizeAbort(TwoPhaseTransaction gtx, String sourceVote, String destVote,
                                               String reason, String idempotencyKey) {
        log.warn("[2PC-PHASE2] Aborting GTX {}. Reason: {}", gtx.getGlobalTransactionId(), reason);

        gtx.setPhase1SourceVote(sourceVote);
        gtx.setPhase1DestVote(destVote);
        gtx.setDecision("GLOBAL_ABORT");
        gtx.setState("ABORTED");
        gtx.setLamportTimestamp(lamportClock.tick());
        twoPhaseRepository.save(gtx);

        // Broadcast abort to participants to release any prepared reservations
        BranchServiceRemote sourceStub = getBranchStub(gtx.getSourceBranchId());
        BranchServiceRemote destStub = getBranchStub(gtx.getDestBranchId());

        if (sourceStub != null) {
            try {
                sourceStub.commitTransaction(new CommitRequest(gtx.getGlobalTransactionId(), "GLOBAL_ABORT", lamportClock.tick()));
            } catch (Exception ignored) {}
        }
        if (destStub != null) {
            try {
                destStub.commitTransaction(new CommitRequest(gtx.getGlobalTransactionId(), "GLOBAL_ABORT", lamportClock.tick()));
            } catch (Exception ignored) {}
        }

        Transaction txn = new Transaction(
                gtx.getGlobalTransactionId(),
                idempotencyKey,
                gtx.getSourceAccount(),
                gtx.getDestAccount(),
                "TRANSFER_DISTRIBUTED",
                gtx.getAmount(),
                gtx.getSourceBranchId(),
                gtx.getDestBranchId(),
                "ABORTED",
                reason,
                lamportClock.tick()
        );
        transactionRepository.save(txn);

        return gtx;
    }

    public void setActiveCoordinatorId(Long newId) {
        this.activeCoordinatorId = newId;
    }

    public Long getActiveCoordinatorId() {
        return activeCoordinatorId;
    }

    public void simulateFailure(boolean fail) {
        this.simulatedFailure = fail;
    }

    public boolean isSimulatedFailure() {
        return simulatedFailure;
    }
}
