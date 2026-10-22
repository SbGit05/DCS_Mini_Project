package com.distributed.banking.controller;

import com.distributed.banking.distributed.BullyElectionService;
import com.distributed.banking.distributed.ReplicationManager;
import com.distributed.banking.distributed.TwoPhaseCommitCoordinator;
import com.distributed.banking.model.Branch;
import com.distributed.banking.model.User;
import com.distributed.banking.repository.BranchRepository;
import com.distributed.banking.rmi.BranchServiceRemote;
import com.distributed.banking.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/distributed")
public class DistributedController {

    private final AuthService authService;
    private final TwoPhaseCommitCoordinator coordinator;
    private final BullyElectionService electionService;
    private final ReplicationManager replicationManager;
    private final BranchRepository branchRepository;

    public DistributedController(AuthService authService,
                                 TwoPhaseCommitCoordinator coordinator,
                                 BullyElectionService electionService,
                                 ReplicationManager replicationManager,
                                 BranchRepository branchRepository) {
        this.authService = authService;
        this.coordinator = coordinator;
        this.electionService = electionService;
        this.replicationManager = replicationManager;
        this.branchRepository = branchRepository;
    }

    private void authenticateAdmin(String authHeader) {
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7).trim() : authHeader;
        User user = authService.validateToken(token);
        if (!"ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Forbidden: Administrator privileges required.");
        }
    }

    /**
     * Explicit node lifecycle state transition (RUNNING, STOPPED, CRASHED, RECOVERING).
     */
    @PostMapping("/node/{nodeId}/state")
    public ResponseEntity<?> setNodeState(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                         @PathVariable Long nodeId,
                                         @RequestParam String state) {
        try {
            authenticateAdmin(authHeader);
            String normalizedState = state.toUpperCase().trim();

            BranchServiceRemote stub = coordinator.getBranchStub(nodeId);
            if (stub != null) {
                stub.setNodeState(normalizedState);

                // If the stopped/crashed node was the active coordinator, handle coordinator availability
                if (Branch.STATE_STOPPED.equalsIgnoreCase(normalizedState) || Branch.STATE_CRASHED.equalsIgnoreCase(normalizedState)) {
                    if (nodeId.equals(coordinator.getActiveCoordinatorId())) {
                        coordinator.setActiveCoordinatorId(null);
                    }
                    if (nodeId.equals(electionService.getCurrentLeaderId())) {
                        electionService.setCurrentLeaderId(null);
                    }
                } else if (Branch.STATE_RUNNING.equalsIgnoreCase(normalizedState)) {
                    if (nodeId.equals(electionService.getCurrentLeaderId())) {
                        coordinator.setActiveCoordinatorId(nodeId);
                    }
                    coordinator.simulateFailure(false);
                }

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "nodeId", nodeId,
                        "state", normalizedState,
                        "message", "Node " + nodeId + " lifecycle state transitioned to " + normalizedState
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Node " + nodeId + " stub unavailable."));
            }
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/node/{nodeId}/stop")
    public ResponseEntity<?> stopNode(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                      @PathVariable Long nodeId) {
        return setNodeState(authHeader, nodeId, Branch.STATE_STOPPED);
    }

    @PostMapping("/node/{nodeId}/crash")
    public ResponseEntity<?> crashNode(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @PathVariable Long nodeId) {
        return setNodeState(authHeader, nodeId, Branch.STATE_CRASHED);
    }

    @PostMapping("/node/{nodeId}/start")
    public ResponseEntity<?> startNode(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @PathVariable Long nodeId) {
        return setNodeState(authHeader, nodeId, Branch.STATE_RUNNING);
    }

    @PostMapping("/node/{nodeId}/recover")
    public ResponseEntity<?> recoverNode(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                         @PathVariable Long nodeId) {
        try {
            authenticateAdmin(authHeader);
            BranchServiceRemote stub = coordinator.getBranchStub(nodeId);
            if (stub != null) {
                // Recover: transition through RECOVERING -> reconcile replicas -> RUNNING
                stub.setNodeState(Branch.STATE_RECOVERING);
                int reconciledCount = replicationManager.reconcileReconnectedNode(nodeId);
                stub.setNodeState(Branch.STATE_RUNNING);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "nodeId", nodeId,
                        "state", Branch.STATE_RUNNING,
                        "reconciledRecords", reconciledCount,
                        "message", "Node " + nodeId + " recovered and reconciled " + reconciledCount + " audit records."
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Node " + nodeId + " stub unavailable."));
            }
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/node/{nodeId}/simulate-failure")
    public ResponseEntity<?> simulateNodeFailure(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                 @PathVariable Long nodeId,
                                                 @RequestParam(defaultValue = "true") boolean fail) {
        return setNodeState(authHeader, nodeId, fail ? Branch.STATE_CRASHED : Branch.STATE_RUNNING);
    }

    @PostMapping("/coordinator/simulate-failure")
    public ResponseEntity<?> simulateCoordinatorFailure(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                        @RequestParam(defaultValue = "true") boolean fail) {
        try {
            authenticateAdmin(authHeader);
            coordinator.simulateFailure(fail);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "simulatedFailure", fail,
                    "message", "2PC Coordinator status set to " + (fail ? "UNAVAILABLE" : "ONLINE")
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/election/start")
    public ResponseEntity<?> triggerBullyElection(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                  @RequestParam(defaultValue = "1") Long initiatorId) {
        try {
            authenticateAdmin(authHeader);
            Map<String, Object> electionResult = electionService.startElection(initiatorId);
            return ResponseEntity.ok(electionResult);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("success", false, "error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Election error: " + e.getMessage()));
        }
    }

    @GetMapping("/election/history")
    public ResponseEntity<?> getElectionHistory(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticateAdmin(authHeader);
            return ResponseEntity.ok(electionService.getElectionEventHistory());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/replication/reconcile")
    public ResponseEntity<?> triggerReplicationReconcile(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                         @RequestParam Long branchId) {
        try {
            authenticateAdmin(authHeader);
            int reconciledCount = replicationManager.reconcileReconnectedNode(branchId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "branchId", branchId,
                    "reconciledRecords", reconciledCount,
                    "message", "Reconciled " + reconciledCount + " audit records for branch " + branchId
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
