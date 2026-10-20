package com.distributed.banking;

import com.distributed.banking.distributed.BullyElectionService;
import com.distributed.banking.distributed.TwoPhaseCommitCoordinator;
import com.distributed.banking.model.Branch;
import com.distributed.banking.repository.BranchRepository;
import com.distributed.banking.rmi.BranchServiceRemote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BullyElectionTest {

    @Autowired
    private BullyElectionService bullyElectionService;

    @Autowired
    private TwoPhaseCommitCoordinator coordinator;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    @DisplayName("Scenario 15: Bully Leader Election Algorithm Execution")
    void testBullyElectionExecution() throws Exception {
        // Test 1: Node 1 initiates election when all nodes are healthy
        Map<String, Object> result = bullyElectionService.startElection(1L);

        assertNotNull(result);
        assertEquals(1L, result.get("initiatorNodeId"));
        // Since Node 3 is highest ID and active, Node 3 should win
        assertEquals(3L, result.get("electedLeaderId"));
        assertEquals(3L, bullyElectionService.getCurrentLeaderId());
        assertEquals(3L, coordinator.getActiveCoordinatorId());

        // Test 2: Simulate failure of highest node (Node 3) and initiate election from Node 1
        BranchServiceRemote node3Stub = coordinator.getBranchStub(3L);
        assertNotNull(node3Stub);

        try {
            node3Stub.simulateFailure(true);

            Map<String, Object> failoverResult = bullyElectionService.startElection(1L);
            // Since Node 3 is offline, Node 2 (next highest) must win the election!
            assertEquals(2L, failoverResult.get("electedLeaderId"), "Node 2 must win when Node 3 is offline");
            assertEquals(2L, coordinator.getActiveCoordinatorId());

            Branch node2 = branchRepository.findById(2L).orElseThrow();
            assertTrue(node2.getIsCoordinator(), "Node 2 must be marked coordinator in database");

        } finally {
            // Restore Node 3 to ONLINE
            node3Stub.simulateFailure(false);
            bullyElectionService.startElection(1L); // Restore Node 3 as leader
        }
    }
}
