package com.distributed.banking;

import com.distributed.banking.distributed.BullyElectionService;
import com.distributed.banking.distributed.TwoPhaseCommitCoordinator;
import com.distributed.banking.model.Branch;
import com.distributed.banking.repository.BranchRepository;
import com.distributed.banking.rmi.BranchServiceRemote;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BullyElectionTest {

    @Autowired
    private BullyElectionService bullyElectionService;

    @Autowired
    private TwoPhaseCommitCoordinator coordinator;

    @Autowired
    private BranchRepository branchRepository;

    @BeforeEach
    @AfterEach
    void resetClusterNodes() throws Exception {
        // Ensure all 3 nodes are in RUNNING state
        for (long i = 1L; i <= 3L; i++) {
            BranchServiceRemote stub = coordinator.getBranchStub(i);
            if (stub != null) {
                try {
                    stub.setNodeState(Branch.STATE_RUNNING);
                } catch (Exception ignored) {}
            }
            final long id = i;
            branchRepository.findById(id).ifPresent(b -> {
                b.setStatus(Branch.STATE_RUNNING);
                b.setIsCoordinator(id == 3L);
                branchRepository.save(b);
            });
        }
        coordinator.simulateFailure(false);
        coordinator.setActiveCoordinatorId(3L);
        bullyElectionService.setCurrentLeaderId(3L);
    }

    @Test
    @DisplayName("Test 1: All nodes running - highest-ID node (Node 3) wins")
    void testAllNodesRunningHighestIdWins() {
        Map<String, Object> result = bullyElectionService.startElection(1L);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertEquals(1L, result.get("initiatorNodeId"));
        assertEquals(3L, result.get("electedLeaderId"));
        assertEquals(3L, bullyElectionService.getCurrentLeaderId());
        assertEquals(3L, coordinator.getActiveCoordinatorId());

        Branch node3 = branchRepository.findById(3L).orElseThrow();
        assertTrue(node3.getIsCoordinator());
        Branch node1 = branchRepository.findById(1L).orElseThrow();
        assertFalse(node1.getIsCoordinator());
    }

    @Test
    @DisplayName("Test 2 [Scenario A]: Node 3 is STOPPED - Node 2 initiates and wins; Node 3 is excluded")
    void testScenarioA_Node3Stopped_Node2Wins() throws Exception {
        BranchServiceRemote node3Stub = coordinator.getBranchStub(3L);
        assertNotNull(node3Stub);
        node3Stub.setNodeState(Branch.STATE_STOPPED);

        Map<String, Object> result = bullyElectionService.startElection(2L);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertEquals(2L, result.get("initiatorNodeId"));
        assertEquals(2L, result.get("electedLeaderId"), "Node 2 must win when Node 3 is STOPPED");
        assertNotEquals(3L, result.get("electedLeaderId"), "Node 3 must NEVER be elected when STOPPED");
        assertEquals(2L, bullyElectionService.getCurrentLeaderId());
        assertEquals(2L, coordinator.getActiveCoordinatorId());

        Branch node2 = branchRepository.findById(2L).orElseThrow();
        assertTrue(node2.getIsCoordinator());
        Branch node3 = branchRepository.findById(3L).orElseThrow();
        assertFalse(node3.getIsCoordinator());
    }

    @Test
    @DisplayName("Test 3 [Scenario B]: Node 3 is CRASHED - Node 1 initiates and Node 2 wins")
    void testScenarioB_Node3Crashed_Node1Initiates_Node2Wins() throws Exception {
        BranchServiceRemote node3Stub = coordinator.getBranchStub(3L);
        assertNotNull(node3Stub);
        node3Stub.setNodeState(Branch.STATE_CRASHED);

        Map<String, Object> result = bullyElectionService.startElection(1L);

        assertNotNull(result);
        assertEquals(2L, result.get("electedLeaderId"), "Node 2 must win when Node 3 is CRASHED");
        assertNotEquals(3L, result.get("electedLeaderId"), "Crashed Node 3 must not be coordinator");
        assertEquals(2L, bullyElectionService.getCurrentLeaderId());
    }

    @Test
    @DisplayName("Test 4 [Scenario C]: Attempt election from Node 3 while STOPPED - Request rejected")
    void testScenarioC_AttemptElectionFromStoppedInitiator_Rejected() throws Exception {
        BranchServiceRemote node3Stub = coordinator.getBranchStub(3L);
        assertNotNull(node3Stub);
        node3Stub.setNodeState(Branch.STATE_STOPPED);

        // Pre-condition: Set leader to Node 2
        bullyElectionService.setCurrentLeaderId(2L);
        coordinator.setActiveCoordinatorId(2L);

        // Attempt election from stopped Node 3
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            bullyElectionService.startElection(3L);
        });

        assertTrue(ex.getMessage().contains("Election cannot be started: Node 3 is STOPPED"),
                "Exception message must clearly explain rejection: " + ex.getMessage());

        // Coordinator must remain unchanged and never be Node 3
        assertEquals(2L, bullyElectionService.getCurrentLeaderId(), "Coordinator must remain unchanged after rejected election");
        assertEquals(2L, coordinator.getActiveCoordinatorId());
        Branch node3 = branchRepository.findById(3L).orElseThrow();
        assertFalse(node3.getIsCoordinator(), "Stopped Node 3 must not be marked coordinator in DB");
    }

    @Test
    @DisplayName("Test 5: Attempt election from Node 3 while CRASHED - Request rejected")
    void testAttemptElectionFromCrashedInitiator_Rejected() throws Exception {
        BranchServiceRemote node3Stub = coordinator.getBranchStub(3L);
        assertNotNull(node3Stub);
        node3Stub.setNodeState(Branch.STATE_CRASHED);

        bullyElectionService.setCurrentLeaderId(2L);
        coordinator.setActiveCoordinatorId(2L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            bullyElectionService.startElection(3L);
        });

        assertTrue(ex.getMessage().contains("Election cannot be started: Node 3 is CRASHED"),
                "Exception message must clearly explain rejection: " + ex.getMessage());
        assertEquals(2L, bullyElectionService.getCurrentLeaderId());
    }

    @Test
    @DisplayName("Test 6 [Scenario D]: All nodes unavailable - Election fails and coordinator is cleared")
    void testScenarioD_AllNodesUnavailable_NoWinnerSelected() throws Exception {
        coordinator.getBranchStub(1L).setNodeState(Branch.STATE_STOPPED);
        coordinator.getBranchStub(2L).setNodeState(Branch.STATE_CRASHED);
        coordinator.getBranchStub(3L).setNodeState(Branch.STATE_STOPPED);

        // Initiating election from any node must be rejected
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            bullyElectionService.startElection(1L);
        });

        assertTrue(ex.getMessage().contains("Election cannot be started: Node 1 is STOPPED"));
    }

    @Test
    @DisplayName("Test 7 [Scenario E]: Highest-ID node is STOPPED - Event log documents exclusion")
    void testScenarioE_HighestIdNodeUnavailable_LogContainsReason() throws Exception {
        BranchServiceRemote node3Stub = coordinator.getBranchStub(3L);
        assertNotNull(node3Stub);
        node3Stub.setNodeState(Branch.STATE_STOPPED);

        Map<String, Object> result = bullyElectionService.startElection(2L);
        assertEquals(2L, result.get("electedLeaderId"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) result.get("electionSteps");
        assertNotNull(steps);

        boolean exclusionLogged = steps.stream().anyMatch(s ->
                "NODE_EXCLUDED".equals(s.get("eventType")) &&
                        Long.valueOf(3L).equals(s.get("nodeId")) &&
                        s.get("details").toString().contains("Node 3 did not respond because it is STOPPED"));

        assertTrue(exclusionLogged, "Election event timeline must log that Node 3 was excluded because it is STOPPED");
    }

    @Test
    @DisplayName("Test 8 [Scenario F]: Node fails during election - Handled gracefully without electing failed node")
    void testScenarioF_NodeFailsDuringElection_GracefullyHandled() throws Exception {
        // Node 3 crashes before responding
        coordinator.getBranchStub(3L).setNodeState(Branch.STATE_CRASHED);

        Map<String, Object> result = bullyElectionService.startElection(2L);
        assertEquals(2L, result.get("electedLeaderId"), "Node 2 must win safely when Node 3 has crashed");

        Branch winner = branchRepository.findById(2L).orElseThrow();
        assertTrue(winner.getIsCoordinator());
        assertTrue(coordinator.getBranchStub(2L).isNodeAlive());
    }

    @Test
    @DisplayName("Test 9 [Scenario G]: Node recovery - Does not automatically make it coordinator until election")
    void testScenarioG_NodeRecoveryDoesNotAutomaticallyMakeItCoordinator() throws Exception {
        // Step 1: Node 3 is stopped, Node 2 is elected coordinator
        coordinator.getBranchStub(3L).setNodeState(Branch.STATE_STOPPED);
        bullyElectionService.startElection(1L);
        assertEquals(2L, bullyElectionService.getCurrentLeaderId());
        assertEquals(2L, coordinator.getActiveCoordinatorId());

        // Step 2: Restart Node 3 (transition through RECOVERING -> RUNNING)
        BranchServiceRemote node3Stub = coordinator.getBranchStub(3L);
        node3Stub.setNodeState(Branch.STATE_RECOVERING);
        node3Stub.setNodeState(Branch.STATE_RUNNING);

        // Node 2 must STILL be the coordinator; Node 3 cannot automatically hijack leadership
        assertEquals(2L, bullyElectionService.getCurrentLeaderId(), "Restarted Node 3 must not automatically become coordinator");
        assertEquals(2L, coordinator.getActiveCoordinatorId());
        Branch node3Db = branchRepository.findById(3L).orElseThrow();
        assertFalse(node3Db.getIsCoordinator(), "Recovered Node 3 is not coordinator until new election completes");

        // Step 3: Run election now that Node 3 is RUNNING -> Node 3 legitimately wins
        Map<String, Object> newElection = bullyElectionService.startElection(1L);
        assertEquals(3L, newElection.get("electedLeaderId"), "Node 3 legitimately wins new election once recovered");
        assertEquals(3L, bullyElectionService.getCurrentLeaderId());
    }

    @Test
    @DisplayName("Test 10: Coordinator is always eligible and responsive after election")
    void testCoordinatorAlwaysEligibleAndResponsiveAfterElection() throws Exception {
        bullyElectionService.startElection(1L);
        Long leaderId = bullyElectionService.getCurrentLeaderId();
        assertNotNull(leaderId);

        Branch leaderBranch = branchRepository.findById(leaderId).orElseThrow();
        assertTrue(leaderBranch.isRunning(), "Elected leader must have RUNNING status in DB");
        assertTrue(coordinator.getBranchStub(leaderId).isNodeAlive(), "Elected leader stub must be responsive");
        assertTrue(coordinator.isCoordinatorAlive(), "TwoPhaseCommitCoordinator must report coordinator alive");
    }

    @Test
    @DisplayName("Test 11: Repeated elections produce consistent, valid results")
    void testRepeatedElectionsProduceConsistentValidResults() {
        for (int i = 0; i < 5; i++) {
            Map<String, Object> result = bullyElectionService.startElection(1L);
            assertEquals(3L, result.get("electedLeaderId"));
            assertEquals(3L, bullyElectionService.getCurrentLeaderId());
        }
    }

    @Test
    @DisplayName("Test 12: Concurrent elections do not produce conflicting final coordinator states")
    void testConcurrentElectionsProduceConsistentState() throws Exception {
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<Map<String, Object>>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final long initiator = (i % 2) + 1; // Alternating 1 and 2
            futures.add(executor.submit(() -> {
                latch.await();
                return bullyElectionService.startElection(initiator);
            }));
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        Long expectedLeader = 3L;
        for (Future<Map<String, Object>> f : futures) {
            Map<String, Object> res = f.get();
            assertEquals(expectedLeader, res.get("electedLeaderId"));
        }

        assertEquals(expectedLeader, bullyElectionService.getCurrentLeaderId());
        assertEquals(expectedLeader, coordinator.getActiveCoordinatorId());
    }
}
