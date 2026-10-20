package com.distributed.banking.controller;

import com.distributed.banking.distributed.BullyElectionService;
import com.distributed.banking.distributed.ReplicationManager;
import com.distributed.banking.distributed.TwoPhaseCommitCoordinator;
import com.distributed.banking.model.User;
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

    public DistributedController(AuthService authService,
                                 TwoPhaseCommitCoordinator coordinator,
                                 BullyElectionService electionService,
                                 ReplicationManager replicationManager) {
        this.authService = authService;
        this.coordinator = coordinator;
        this.electionService = electionService;
        this.replicationManager = replicationManager;
    }

    private void authenticateAdmin(String authHeader) {
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7).trim() : authHeader;
        User user = authService.validateToken(token);
        if (!"ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Forbidden: Administrator privileges required.");
        }
    }

    @PostMapping("/node/{nodeId}/simulate-failure")
    public ResponseEntity<?> simulateNodeFailure(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                 @PathVariable Long nodeId,
                                                 @RequestParam(defaultValue = "true") boolean fail) {
        try {
            authenticateAdmin(authHeader);
            BranchServiceRemote stub = coordinator.getBranchStub(nodeId);
            if (stub != null) {
                stub.simulateFailure(fail);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "nodeId", nodeId,
                        "simulatedFailure", fail,
                        "message", "Node " + nodeId + " status updated to " + (fail ? "UNAVAILABLE" : "ONLINE")
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Node " + nodeId + " not found or stub unavailable."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
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
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/election/history")
    public ResponseEntity<?> getElectionHistory(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticateAdmin(authHeader);
            return ResponseEntity.ok(electionService.getElectionEventHistory());
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
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
