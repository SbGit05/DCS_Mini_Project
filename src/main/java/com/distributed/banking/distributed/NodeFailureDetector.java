package com.distributed.banking.distributed;

import com.distributed.banking.model.Branch;
import com.distributed.banking.model.LamportEvent;
import com.distributed.banking.repository.BranchRepository;
import com.distributed.banking.repository.LamportEventRepository;
import com.distributed.banking.rmi.BranchServiceRemote;
import com.distributed.banking.rmi.dto.HeartbeatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distributed Heartbeat Monitor and Failure Detector.
 * Periodically pings all branch nodes over RMI, updates liveness states,
 * and triggers automated reconciliation when an unavailable node recovers.
 */
@Component
public class NodeFailureDetector {

    private static final Logger log = LoggerFactory.getLogger(NodeFailureDetector.class);

    private final BranchRepository branchRepository;
    private final LamportEventRepository eventRepository;
    private final LamportClock lamportClock;
    private final ReplicationManager replicationManager;
    private final TwoPhaseCommitCoordinator coordinator;
    private final BullyElectionService electionService;

    private final Map<Long, BranchServiceRemote> stubs = new ConcurrentHashMap<>();
    private final Map<Long, Integer> consecutiveFailures = new ConcurrentHashMap<>();

    public NodeFailureDetector(BranchRepository branchRepository,
                               LamportEventRepository eventRepository,
                               LamportClock lamportClock,
                               ReplicationManager replicationManager,
                               @org.springframework.context.annotation.Lazy TwoPhaseCommitCoordinator coordinator,
                               @org.springframework.context.annotation.Lazy BullyElectionService electionService) {
        this.branchRepository = branchRepository;
        this.eventRepository = eventRepository;
        this.lamportClock = lamportClock;
        this.replicationManager = replicationManager;
        this.coordinator = coordinator;
        this.electionService = electionService;
    }

    public void registerStub(Long nodeId, BranchServiceRemote stub) {
        stubs.put(nodeId, stub);
    }

    /**
     * Heartbeat check runs every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 8000)
    public void monitorClusterNodes() {
        List<Branch> branches = branchRepository.findAll();
        for (Branch b : branches) {
            checkNodeHealth(b);
        }
    }

    public synchronized void checkNodeHealth(Branch b) {
        Long nodeId = b.getId();
        BranchServiceRemote stub = stubs.get(nodeId);

        if (stub == null) {
            return;
        }

        try {
            HeartbeatMessage ping = new HeartbeatMessage(0L, "HealthMonitor", false, "PING", lamportClock.tick());
            HeartbeatMessage pong = stub.ping(ping);

            if (pong != null && (Branch.STATE_RUNNING.equalsIgnoreCase(pong.getStatus()) || "ONLINE".equalsIgnoreCase(pong.getStatus()))) {
                consecutiveFailures.put(nodeId, 0);

                if ("UNAVAILABLE".equalsIgnoreCase(b.getStatus()) ||
                    Branch.STATE_CRASHED.equalsIgnoreCase(b.getStatus()) ||
                    Branch.STATE_RECOVERING.equalsIgnoreCase(b.getStatus())) {

                    log.info("[HEARTBEAT-RECOVERY] Node {} has come back ONLINE/RUNNING. Triggering replica reconciliation...", nodeId);
                    b.setStatus(Branch.STATE_RECOVERING);
                    branchRepository.save(b);

                    // Reconcile pending replicas
                    int reconciled = replicationManager.reconcileReconnectedNode(nodeId);

                    b.setStatus(Branch.STATE_RUNNING);
                    b.setLastHeartbeat(LocalDateTime.now());
                    branchRepository.save(b);

                    LamportEvent event = new LamportEvent(nodeId, b.getBranchName(), lamportClock.tick(),
                            "NODE_RECOVERY", "Node " + nodeId + " recovered and reconciled " + reconciled + " pending audit records.");
                    eventRepository.save(event);
                } else {
                    b.setLastHeartbeat(LocalDateTime.now());
                    branchRepository.save(b);
                }
            }
        } catch (Exception e) {
            int fails = consecutiveFailures.getOrDefault(nodeId, 0) + 1;
            consecutiveFailures.put(nodeId, fails);

            // Only transition to CRASHED if node was running (not if intentionally STOPPED)
            if (fails >= 2 && b.isRunning()) {
                log.warn("[HEARTBEAT-FAILURE] Node {} detected unresponsive after {} consecutive ping timeouts: {}",
                        nodeId, fails, e.getMessage());
                b.setStatus(Branch.STATE_CRASHED);
                b.setIsCoordinator(false);
                branchRepository.save(b);

                if (nodeId.equals(coordinator.getActiveCoordinatorId())) {
                    coordinator.setActiveCoordinatorId(null);
                }
                if (nodeId.equals(electionService.getCurrentLeaderId())) {
                    electionService.setCurrentLeaderId(null);
                }

                LamportEvent event = new LamportEvent(nodeId, b.getBranchName(), lamportClock.tick(),
                        "NODE_FAILURE", "Failure detector marked Node " + nodeId + " as CRASHED: " + e.getMessage());
                eventRepository.save(event);
            }
        }
    }
}
