package com.distributed.banking.controller;

import com.distributed.banking.model.User;
import com.distributed.banking.service.AccountService;
import com.distributed.banking.service.AuditService;
import com.distributed.banking.service.AuthService;
import com.distributed.banking.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AuthService authService;
    private final AuditService auditService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    public AdminController(AuthService authService,
                           AuditService auditService,
                           AccountService accountService,
                           TransactionService transactionService) {
        this.authService = authService;
        this.auditService = auditService;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    private void authenticateAdmin(String authHeader) {
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7).trim() : authHeader;
        User user = authService.validateToken(token);
        if (!"ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Forbidden: Administrator privileges required.");
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticateAdmin(authHeader);
            Map<String, Object> dashboardData = auditService.getSystemDashboard();
            return ResponseEntity.ok(dashboardData);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> getAllAccounts(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticateAdmin(authHeader);
            return ResponseEntity.ok(accountService.getAllAccounts());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticateAdmin(authHeader);
            return ResponseEntity.ok(transactionService.getAllRecentTransactions());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/2pc-transactions")
    public ResponseEntity<?> getTwoPhaseTransactions(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticateAdmin(authHeader);
            return ResponseEntity.ok(auditService.getTwoPhaseTransactions());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/lamport-logs")
    public ResponseEntity<?> getLamportLogs(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticateAdmin(authHeader);
            return ResponseEntity.ok(auditService.getLamportEvents());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/replication-records")
    public ResponseEntity<?> getReplicationRecords(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            authenticateAdmin(authHeader);
            return ResponseEntity.ok(auditService.getReplicationAuditRecords());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}
