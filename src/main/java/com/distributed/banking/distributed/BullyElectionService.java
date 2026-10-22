package com.distributed.banking.distributed;

import com.distributed.banking.model.Branch;
import com.distributed.banking.model.LamportEvent;
import com.distributed.banking.repository.BranchRepository;
import com.distributed.banking.repository.LamportEventRepository;
import com.distributed.banking.rmi.BranchServiceRemote;
import com.distributed.banking.rmi.dto.ElectionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * Standard implementation of the Bully Leader Election Algorithm
 * with strict node liveness verification and rejection of unavailable initiators.
 *
 * Algorithm Rules:
 * 1. Process P notices coordinator failure and initiates an election.
 *    P must be RUNNING and eligible; an unavailable initiator is rejected immediately.
 * 2. P sends an ELECTION message to all processes with higher IDs (P_higher > P).
 * 3. Unavailable (STOPPED, CRASHED) nodes are excluded and do not respond.
 * 4. If a higher-ID eligible node responds with OK, that node takes over the election.
 * 5. If no higher-ID eligible node responds, P wins the election and sends a COORDINATOR
 *    announcement to all cluster nodes.
 * 6. Under no circumstances can a STOPPED or CRASHED node become the coordinator.
 */
@Component
public class BullyElectionService {

    private static final Logger log = LoggerFactory.getLogger(BullyElectionService.class);

    private final BranchRepository branchRepository;
    private final LamportEventRepository eventRepository;
    private final LamportClock lamportClock;
    private final TwoPhaseCommitCoordinator coordinator;

    private final Map<Long, BranchServiceRemote> nodeStubs = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> electionEventHistory = new CopyOnWriteArrayList<>();

    private volatile Long currentLeaderId = 3L; // Initially Node 3 is leader if running

    public BullyElectionService(BranchRepository branchRepository,
                                LamportEventRepository eventRepository,
                                LamportClock lamportClock,
                                TwoPhaseCommitCoordinator coordinator) {
        this.branchRepository = branchRepository;
        this.eventRepository = eventRepository;
        this.lamportClock = lamportClock;
        this.coordinator = coordinator;
    }

    public void registerStub(Long nodeId, BranchServiceRemote stub) {
        nodeStubs.put(nodeId, stub);
    }

    /**
     * Initiates the Bully election algorithm starting from initiatorNodeId.
     * Enforces strict liveness validation on the initiator and participant nodes.
     */
    public synchronized Map<String, Object> startElection(Long initiatorNodeId) {
        electionEventHistory.clear();
        long startTs = lamportClock.tick();

        // 1. Verify that the initiating node exists
        Branch initiator = branchRepository.findById(initiatorNodeId).orElse(null);
        if (initiator == null) {
            String err = "Election cannot be started: Node " + initiatorNodeId + " does not exist.";
            recordElectionStep("ELECTION_REJECTED", initiatorNodeId, err, startTs);
            throw new IllegalArgumentException(err);
        }

        // 2. Verify that the initiating node is currently RUNNING and eligible
        BranchServiceRemote initiatorStub = nodeStubs.get(initiatorNodeId);
        boolean isStubAlive = false;
        if (initiatorStub != null) {
            try {
                isStubAlive = initiatorStub.isNodeAlive();
            } catch (Exception ignored) {
                isStubAlive = false;
            }
        }

        if (!initiator.isRunning() || !isStubAlive) {
            String status = initiator.getStatus() != null ? initiator.getStatus() : "UNAVAILABLE";
            String err = "Election cannot be started: Node " + initiatorNodeId + " is " + status + ".";
            recordElectionStep("ELECTION_REJECTED", initiatorNodeId, err, startTs);
            log.warn("[BULLY-ELECTION-REJECTED] Node {} tried to initiate election but is {}", initiatorNodeId, status);
            throw new IllegalStateException(err);
        }

        log.info("[BULLY-ELECTION] Node {} initiated Bully Election. L(e)={}", initiatorNodeId, startTs);
        recordElectionStep("ELECTION_INITIATED", initiatorNodeId,
                "Election requested by Node " + initiatorNodeId + " (" + initiator.getBranchName() + ")", startTs);

        // 3. Retrieve authoritative current state of all nodes from backend
        List<Branch> allBranches = branchRepository.findAllByOrderByIdAsc();

        // Check if ANY eligible running nodes exist
        List<Branch> eligibleNodes = allBranches.stream()
                .filter(b -> {
                    if (!b.isRunning()) return false;
                    BranchServiceRemote stub = nodeStubs.get(b.getId());
                    if (stub == null) return false;
                    try {
                        return stub.isNodeAlive();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();

        if (eligibleNodes.isEmpty()) {
            String err = "Election failed: No eligible nodes are available.";
            recordElectionStep("ELECTION_FAILED", initiatorNodeId, err, lamportClock.tick());
            this.currentLeaderId = null;
            coordinator.setActiveCoordinatorId(null);
            for (Branch b : allBranches) {
                b.setIsCoordinator(false);
                branchRepository.save(b);
            }
            throw new IllegalStateException(err);
        }

        // 4. Bully Algorithm Execution:
        // Initiator sends ELECTION messages to all nodes with higher IDs
        List<Branch> higherNodes = allBranches.stream()
                .filter(b -> b.getId() > initiatorNodeId)
                .toList();

        Long highestRespondingNode = null;

        if (higherNodes.isEmpty()) {
            recordElectionStep("NO_HIGHER_NODES", initiatorNodeId,
                    "No higher-ID nodes exist in the cluster. Node " + initiatorNodeId + " is the highest node.", lamportClock.tick());
        } else {
            for (Branch higher : higherNodes) {
                long msgTs = lamportClock.tick();
                recordElectionStep("ELECTION_SENT", initiatorNodeId,
                        "Node " + initiatorNodeId + " sent an election message to Node " + higher.getId() + " (" + higher.getBranchName() + ")", msgTs);

                BranchServiceRemote stub = nodeStubs.get(higher.getId());
                boolean responds = false;

                if (higher.isRunning() && stub != null) {
                    try {
                        if (stub.isNodeAlive()) {
                            ElectionMessage reply = stub.handleElection(
                                    new ElectionMessage(initiatorNodeId, "Initiator-" + initiatorNodeId, ElectionMessage.Type.ELECTION, msgTs)
                            );
                            if (reply != null && reply.getType() == ElectionMessage.Type.OK) {
                                long replyTs = lamportClock.updateAndTick(reply.getLamportTimestamp());
                                responds = true;
                                highestRespondingNode = higher.getId();
                                recordElectionStep("OK_RECEIVED", higher.getId(),
                                        "Node " + higher.getId() + " replied with OK to Node " + initiatorNodeId + ". Node " + higher.getId() + " takes over the election.", replyTs);
                            }
                        }
                    } catch (Exception e) {
                        responds = false;
                    }
                }

                if (!responds) {
                    recordElectionStep("NODE_EXCLUDED", higher.getId(),
                            "Node " + higher.getId() + " did not respond because it is " + higher.getStatus() + " and is excluded from the election.", lamportClock.tick());
                }
            }
        }

        // 5. Determine the winner:
        // If a higher node responded OK, check if there are even higher nodes above it
        Long winnerNodeId;
        if (highestRespondingNode != null) {
            // The highest eligible node that responded takes over
            winnerNodeId = highestRespondingNode;

            // Verify if there are any nodes higher than highestRespondingNode that might be eligible
            final Long candidateNodeId = highestRespondingNode;
            List<Branch> evenHigher = allBranches.stream()
                    .filter(b -> b.getId() > candidateNodeId && b.isRunning())
                    .toList();
            for (Branch eh : evenHigher) {
                BranchServiceRemote ehStub = nodeStubs.get(eh.getId());
                if (ehStub != null) {
                    try {
                        if (ehStub.isNodeAlive()) {
                            winnerNodeId = eh.getId();
                        }
                    } catch (Exception ignored) {}
                }
            }
        } else {
            // No higher node responded -> initiator wins
            winnerNodeId = initiatorNodeId;
        }

        // Final verification: Winner MUST be running and responsive
        Branch winnerBranch = branchRepository.findById(winnerNodeId).orElse(null);
        BranchServiceRemote winnerStub = nodeStubs.get(winnerNodeId);
        boolean isWinnerAlive = false;
        if (winnerStub != null) {
            try {
                isWinnerAlive = winnerStub.isNodeAlive();
            } catch (Exception ignored) {
                isWinnerAlive = false;
            }
        }

        if (winnerBranch == null || !winnerBranch.isRunning() || !isWinnerAlive) {
            // Find highest among eligible nodes
            List<Branch> currentlyEligible = branchRepository.findAllByOrderByIdAsc().stream()
                    .filter(b -> {
                        if (!b.isRunning()) return false;
                        BranchServiceRemote s = nodeStubs.get(b.getId());
                        if (s == null) return false;
                        try {
                            return s.isNodeAlive();
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .toList();

            Optional<Branch> bestEligible = currentlyEligible.stream().max(Comparator.comparingLong(Branch::getId));
            if (bestEligible.isPresent()) {
                winnerNodeId = bestEligible.get().getId();
                recordElectionStep("FAILOVER_RECOVERY", winnerNodeId,
                        "Initial winner became unavailable. Fallback to highest responsive eligible Node " + winnerNodeId + ".", lamportClock.tick());
            } else {
                this.currentLeaderId = null;
                coordinator.setActiveCoordinatorId(null);
                for (Branch b : allBranches) {
                    b.setIsCoordinator(false);
                    branchRepository.save(b);
                }
                throw new IllegalStateException("Election failed: No eligible nodes are available.");
            }
        }

        recordElectionStep("NO_HIGHER_RESPONDED", winnerNodeId,
                "No higher-ID eligible node responded.", lamportClock.tick());

        // 6. Proclaim Winner & Broadcast COORDINATOR
        this.currentLeaderId = winnerNodeId;
        coordinator.setActiveCoordinatorId(winnerNodeId);
        coordinator.simulateFailure(false);

        // Update database coordinator flags
        for (Branch b : allBranches) {
            boolean isWinner = b.getId().equals(winnerNodeId);
            b.setIsCoordinator(isWinner);
            branchRepository.save(b);
        }

        long announceTs = lamportClock.tick();

        // Broadcast COORDINATOR announcement to all active node stubs
        for (Branch b : allBranches) {
            BranchServiceRemote stub = nodeStubs.get(b.getId());
            if (stub != null && b.isRunning()) {
                try {
                    stub.handleElection(new ElectionMessage(winnerNodeId, "Leader-" + winnerNodeId, ElectionMessage.Type.COORDINATOR, announceTs));
                } catch (Exception ignored) {}
            }
        }

        recordElectionStep("COORDINATOR_ANNOUNCED", winnerNodeId,
                "Node " + winnerNodeId + " elected as coordinator and broadcasts COORDINATOR announcement to all nodes.", announceTs);
        recordElectionStep("COORDINATOR_UPDATED", winnerNodeId,
                "Coordinator updated successfully to Node " + winnerNodeId + ".", lamportClock.tick());

        log.info("[BULLY-ELECTION-END] Election won by Node {}. New Cluster Coordinator established. L(e)={}",
                winnerNodeId, announceTs);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("success", true);
        summary.put("initiatorNodeId", initiatorNodeId);
        summary.put("electedLeaderId", winnerNodeId);
        summary.put("lamportTimestamp", announceTs);
        summary.put("electionSteps", new ArrayList<>(electionEventHistory));
        return summary;
    }

    private void recordElectionStep(String eventType, Long nodeId, String details, long ts) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("timestamp", ts);
        step.put("nodeId", nodeId);
        step.put("eventType", eventType);
        step.put("details", details);
        electionEventHistory.add(step);

        try {
            LamportEvent event = new LamportEvent(nodeId, "Node-" + nodeId, ts, eventType, details);
            eventRepository.save(event);
        } catch (Exception ignored) {}
    }

    public Long getCurrentLeaderId() {
        if (currentLeaderId != null) {
            Branch leader = branchRepository.findById(currentLeaderId).orElse(null);
            if (leader == null || !leader.isRunning()) {
                currentLeaderId = null;
            } else {
                BranchServiceRemote stub = nodeStubs.get(currentLeaderId);
                if (stub != null) {
                    try {
                        if (!stub.isNodeAlive()) {
                            currentLeaderId = null;
                        }
                    } catch (Exception e) {
                        currentLeaderId = null;
                    }
                }
            }
        }
        return currentLeaderId;
    }

    public void setCurrentLeaderId(Long id) {
        this.currentLeaderId = id;
    }

    public List<Map<String, Object>> getElectionEventHistory() {
        return electionEventHistory;
    }
}
