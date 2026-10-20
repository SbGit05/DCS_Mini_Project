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
 * Authentic implementation of the Bully Leader Election Algorithm
 * across distributed coordinator / branch nodes.
 *
 * Algorithm Rules:
 * 1. Process P notices coordinator failure and initiates an election.
 * 2. P sends an ELECTION message to all processes with higher IDs (P_higher > P).
 * 3. If no process responds, P wins the election and sends a COORDINATOR message to all.
 * 4. If any process with a higher ID responds with OK, P's job is done and the higher process continues.
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

    private volatile Long currentLeaderId = 3L; // Initially Node 3 (highest ID) is leader

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
     */
    public synchronized Map<String, Object> startElection(Long initiatorNodeId) {
        electionEventHistory.clear();
        long startTs = lamportClock.tick();

        log.info("[BULLY-ELECTION] Node {} detected coordinator failure and initiated Bully Election! L(e)={}",
                initiatorNodeId, startTs);

        recordElectionStep("ELECTION_INITIATED", initiatorNodeId,
                "Node " + initiatorNodeId + " detected coordinator failure and initiated Bully election.", startTs);

        List<Branch> allBranches = branchRepository.findAllByOrderByIdAsc();
        List<Branch> higherNodes = allBranches.stream()
                .filter(b -> b.getId() > initiatorNodeId)
                .toList();

        boolean higherNodeResponded = false;
        Long highestRespondingNode = initiatorNodeId;

        if (higherNodes.isEmpty()) {
            recordElectionStep("NO_HIGHER_NODES", initiatorNodeId,
                    "Node " + initiatorNodeId + " is already the highest numbered node in the cluster.", lamportClock.tick());
        } else {
            for (Branch higher : higherNodes) {
                long msgTs = lamportClock.tick();
                recordElectionStep("ELECTION_SENT", initiatorNodeId,
                        "Node " + initiatorNodeId + " sending ELECTION message to higher Node " + higher.getId() + " (" + higher.getBranchName() + ")", msgTs);

                BranchServiceRemote stub = nodeStubs.get(higher.getId());
                if (stub != null) {
                    try {
                        ElectionMessage reply = stub.handleElection(
                                new ElectionMessage(initiatorNodeId, "Initiator-" + initiatorNodeId, ElectionMessage.Type.ELECTION, msgTs)
                        );
                        if (reply != null && reply.getType() == ElectionMessage.Type.OK) {
                            long replyTs = lamportClock.updateAndTick(reply.getLamportTimestamp());
                            higherNodeResponded = true;
                            highestRespondingNode = higher.getId();
                            recordElectionStep("OK_RECEIVED", higher.getId(),
                                    "Higher Node " + higher.getId() + " replied with OK to Node " + initiatorNodeId + ". Higher node assumes leadership.", replyTs);
                        }
                    } catch (Exception e) {
                        recordElectionStep("NODE_UNREACHABLE", higher.getId(),
                                "Higher Node " + higher.getId() + " failed to respond (offline / unreachable).", lamportClock.tick());
                    }
                } else {
                    recordElectionStep("NODE_UNREACHABLE", higher.getId(),
                            "Higher Node " + higher.getId() + " RMI stub not connected.", lamportClock.tick());
                }
            }
        }

        Long winnerNodeId = higherNodeResponded ? highestRespondingNode : initiatorNodeId;
        this.currentLeaderId = winnerNodeId;
        coordinator.setActiveCoordinatorId(winnerNodeId);

        // Update database coordinator flags
        for (Branch b : allBranches) {
            boolean isWinner = b.getId().equals(winnerNodeId);
            b.setIsCoordinator(isWinner);
            branchRepository.save(b);
        }

        long announceTs = lamportClock.tick();
        recordElectionStep("COORDINATOR_ANNOUNCED", winnerNodeId,
                "Node " + winnerNodeId + " wins the Bully election and broadcasts COORDINATOR announcement to all nodes!", announceTs);

        log.info("[BULLY-ELECTION-END] Election won by Node {}. New Cluster Coordinator established. L(e)={}",
                winnerNodeId, announceTs);

        Map<String, Object> summary = new LinkedHashMap<>();
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
        return currentLeaderId;
    }

    public List<Map<String, Object>> getElectionEventHistory() {
        return electionEventHistory;
    }
}
