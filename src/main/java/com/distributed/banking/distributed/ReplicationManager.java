package com.distributed.banking.distributed;

import com.distributed.banking.model.Branch;
import com.distributed.banking.model.ReplicationRecord;
import com.distributed.banking.repository.BranchRepository;
import com.distributed.banking.repository.ReplicationRecordRepository;
import com.distributed.banking.repository.TransactionRepository;
import com.distributed.banking.rmi.BranchServiceRemote;
import com.distributed.banking.rmi.dto.ReplicationRequest;
import com.distributed.banking.rmi.dto.ReplicationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages branch data replication and reconciliation.
 * Implements sequence tracking, discrepancy detection, and recovery reconciliation
 * as required by Unit 4.
 */
@Component
public class ReplicationManager {

    private static final Logger log = LoggerFactory.getLogger(ReplicationManager.class);

    private final ReplicationRecordRepository replicationRepository;
    private final BranchRepository branchRepository;
    private final TransactionRepository transactionRepository;
    private final LamportClock lamportClock;
    private final AtomicLong globalSequence = new AtomicLong(1000L);

    // Cache of remote branch stubs
    private final Map<Long, BranchServiceRemote> remoteStubs = new ConcurrentHashMap<>();

    public ReplicationManager(ReplicationRecordRepository replicationRepository,
                              BranchRepository branchRepository,
                              TransactionRepository transactionRepository,
                              LamportClock lamportClock) {
        this.replicationRepository = replicationRepository;
        this.branchRepository = branchRepository;
        this.transactionRepository = transactionRepository;
        this.lamportClock = lamportClock;
    }

    public void registerStub(Long branchId, BranchServiceRemote stub) {
        remoteStubs.put(branchId, stub);
    }

    /**
     * Records a transaction replication event and distributes to peer branches.
     */
    public void recordTransaction(Long sourceBranchId, Long targetBranchId, String txnRef,
                                  String accountNumber, String changeType, BigDecimal amount) {
        long seq = globalSequence.incrementAndGet();
        long ts = lamportClock.tick();

        // 1. Persist local replica audit record
        ReplicationRecord record = new ReplicationRecord(
                seq, sourceBranchId, targetBranchId, txnRef, accountNumber, changeType, amount, "SYNCED", ts
        );
        replicationRepository.save(record);

        // 2. Replicate asynchronously to peer branches
        List<Branch> allBranches = branchRepository.findAll();
        for (Branch b : allBranches) {
            if (!b.getId().equals(sourceBranchId)) {
                BranchServiceRemote stub = remoteStubs.get(b.getId());
                if (stub != null) {
                    try {
                        ReplicationRequest req = new ReplicationRequest(
                                sourceBranchId, b.getId(), seq, txnRef, accountNumber, changeType, amount, lamportClock.tick()
                        );
                        ReplicationResponse resp = stub.receiveReplication(req);
                        log.info("Replication ACK from Branch {}: seq={}", b.getId(), resp.getAcknowledgedSequence());
                    } catch (Exception e) {
                        log.warn("Replication delivery to Branch {} failed (node may be offline): {}. Marking PENDING_RECONCILIATION.",
                                b.getId(), e.getMessage());
                        ReplicationRecord pending = new ReplicationRecord(
                                seq, sourceBranchId, b.getId(), txnRef, accountNumber, changeType, amount, "PENDING_RECONCILIATION", ts
                        );
                        replicationRepository.save(pending);
                    }
                }
            }
        }
    }

    /**
     * Detects discrepancies between authoritative records and replica copies.
     */
    public Map<String, Object> checkDiscrepancies() {
        Map<String, Object> result = new LinkedHashMap<>();
        long totalTransactions = transactionRepository.count();
        long totalReplicationRecords = replicationRepository.count();
        long pendingSync = replicationRepository.countBySyncStatus("PENDING_RECONCILIATION");

        result.put("totalAuthoritativeTransactions", totalTransactions);
        result.put("totalReplicaAuditRecords", totalReplicationRecords);
        result.put("pendingReconciliations", pendingSync);
        result.put("status", pendingSync == 0 ? "CONSISTENT" : "DISCREPANCY_DETECTED");
        result.put("reconciliationNeeded", pendingSync > 0);

        return result;
    }

    /**
     * Reconciles pending records for a reconnected branch node.
     */
    public int reconcileReconnectedNode(Long branchId) {
        log.info("[RECONCILIATION] Starting replica reconciliation for reconnected Branch {}...", branchId);
        List<ReplicationRecord> pendingRecords = replicationRepository.findByTargetBranchIdAndSyncStatus(branchId, "PENDING_RECONCILIATION");

        if (pendingRecords.isEmpty()) {
            log.info("[RECONCILIATION] No pending records for Branch {}. Replicas are already synchronized.", branchId);
            return 0;
        }

        BranchServiceRemote stub = remoteStubs.get(branchId);
        if (stub == null) {
            log.warn("[RECONCILIATION] Cannot reconcile Branch {}: remote stub unavailable", branchId);
            return 0;
        }

        int reconciledCount = 0;
        for (ReplicationRecord rec : pendingRecords) {
            try {
                ReplicationRequest req = new ReplicationRequest(
                        rec.getSourceBranchId(), branchId, rec.getSequenceNumber(),
                        rec.getTransactionRef(), rec.getAccountNumber(), rec.getChangeType(),
                        rec.getAmount(), lamportClock.tick()
                );
                ReplicationResponse resp = stub.receiveReplication(req);
                if (resp.isSuccess()) {
                    rec.setSyncStatus("SYNCED");
                    rec.setLamportTimestamp(lamportClock.tick());
                    replicationRepository.save(rec);
                    reconciledCount++;
                }
            } catch (Exception e) {
                log.error("[RECONCILIATION-FAIL] Failed to sync record seq {} to branch {}: {}",
                        rec.getSequenceNumber(), branchId, e.getMessage());
                break; // Stop if node disconnected again
            }
        }

        log.info("[RECONCILIATION] Successfully reconciled {} records for Branch {}.", reconciledCount, branchId);
        return reconciledCount;
    }
}
