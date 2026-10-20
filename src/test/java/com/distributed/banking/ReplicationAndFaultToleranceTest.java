package com.distributed.banking;

import com.distributed.banking.distributed.NodeFailureDetector;
import com.distributed.banking.distributed.ReplicationManager;
import com.distributed.banking.distributed.TwoPhaseCommitCoordinator;
import com.distributed.banking.model.Branch;
import com.distributed.banking.repository.BranchRepository;
import com.distributed.banking.repository.ReplicationRecordRepository;
import com.distributed.banking.rmi.BranchServiceRemote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ReplicationAndFaultToleranceTest {

    @Autowired
    private ReplicationManager replicationManager;

    @Autowired
    private ReplicationRecordRepository replicationRepository;

    @Autowired
    private TwoPhaseCommitCoordinator coordinator;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private NodeFailureDetector failureDetector;

    @Test
    @DisplayName("Scenario 12 & 13: Node Failure Simulation, Pending Replication, and Reconnect Reconciliation")
    void testFailureReplicationAndReconciliation() throws Exception {
        Long branchId = 2L;
        BranchServiceRemote stub = coordinator.getBranchStub(branchId);
        assertNotNull(stub, "Branch 2 RMI stub must be available");

        try {
            // 1. Simulate Branch 2 failure
            stub.simulateFailure(true);
            assertFalse(stub.isNodeAlive());

            // 2. Perform a transaction at Branch 1 that replicates to Branch 2
            String txnRef = "TXN-REP-" + UUID.randomUUID().toString().substring(0, 8);
            replicationManager.recordTransaction(1L, branchId, txnRef, "PUN-1001", "BALANCE_UPDATE", new BigDecimal("750.00"));

            // Check discrepancy detection
            Map<String, Object> discrepancy = replicationManager.checkDiscrepancies();
            assertNotNull(discrepancy);
            assertTrue((Long) discrepancy.get("pendingReconciliations") >= 1, "There should be pending reconciliation records");

            // 3. Bring Branch 2 back ONLINE
            stub.simulateFailure(false);
            assertTrue(stub.isNodeAlive());

            // 4. Trigger reconciliation
            int reconciled = replicationManager.reconcileReconnectedNode(branchId);
            assertTrue(reconciled >= 1, "At least one pending record should be reconciled");

            // 5. Verify discrepancy is cleared
            var pendingAfter = replicationRepository.findByTargetBranchIdAndSyncStatus(branchId, "PENDING_RECONCILIATION");
            assertEquals(0, pendingAfter.size(), "Pending reconciliations for Branch 2 must be zero after recovery");

        } finally {
            stub.simulateFailure(false);
        }
    }
}
