package com.distributed.banking.controller;

import com.distributed.banking.model.Account;
import com.distributed.banking.model.Transaction;
import com.distributed.banking.model.User;
import com.distributed.banking.service.AccountService;
import com.distributed.banking.service.AuthService;
import com.distributed.banking.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final AuthService authService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    public CustomerController(AuthService authService,
                              AccountService accountService,
                              TransactionService transactionService) {
        this.authService = authService;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    private User authenticateCustomer(String authHeader) {
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7).trim() : authHeader;
        User user = authService.validateToken(token);
        if (!"ROLE_CUSTOMER".equalsIgnoreCase(user.getRole()) && !"ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Unauthorized role access.");
        }
        return user;
    }

    @GetMapping("/account")
    public ResponseEntity<?> getCustomerAccounts(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authenticateCustomer(authHeader);
            List<Account> accounts = accountService.getAccountsByUserId(user.getId());
            return ResponseEntity.ok(accounts);
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                     @RequestBody Map<String, Object> req) {
        try {
            User user = authenticateCustomer(authHeader);
            String accountNumber = (String) req.get("accountNumber");
            BigDecimal amount = parseAmount(req.get("amount"));
            String idempotencyKey = (String) req.get("idempotencyKey");

            // Verify account belongs to user (or admin)
            verifyAccountOwnership(user, accountNumber);

            Transaction txn = transactionService.deposit(accountNumber, amount, idempotencyKey);
            BigDecimal newBalance = accountService.getBalance(accountNumber);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "transactionRef", txn.getTransactionRef(),
                    "status", txn.getStatus(),
                    "amount", txn.getAmount(),
                    "newBalance", newBalance,
                    "lamportTimestamp", txn.getLamportTimestamp(),
                    "message", "Deposit completed successfully."
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Transaction error: " + e.getMessage()));
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                      @RequestBody Map<String, Object> req) {
        try {
            User user = authenticateCustomer(authHeader);
            String accountNumber = (String) req.get("accountNumber");
            BigDecimal amount = parseAmount(req.get("amount"));
            String idempotencyKey = (String) req.get("idempotencyKey");

            verifyAccountOwnership(user, accountNumber);

            Transaction txn = transactionService.withdraw(accountNumber, amount, idempotencyKey);
            BigDecimal newBalance = accountService.getBalance(accountNumber);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "transactionRef", txn.getTransactionRef(),
                    "status", txn.getStatus(),
                    "amount", txn.getAmount(),
                    "newBalance", newBalance,
                    "lamportTimestamp", txn.getLamportTimestamp(),
                    "message", "Withdrawal completed successfully."
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Transaction error: " + e.getMessage()));
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                      @RequestBody Map<String, Object> req) {
        try {
            User user = authenticateCustomer(authHeader);
            String fromAccount = (String) req.get("fromAccount");
            String toAccount = (String) req.get("toAccount");
            BigDecimal amount = parseAmount(req.get("amount"));
            String idempotencyKey = (String) req.get("idempotencyKey");

            verifyAccountOwnership(user, fromAccount);

            Transaction txn = transactionService.transfer(fromAccount, toAccount, amount, idempotencyKey);
            BigDecimal newBalance = accountService.getBalance(fromAccount);

            boolean isSuccess = "SUCCESS".equalsIgnoreCase(txn.getStatus());

            return ResponseEntity.ok(Map.of(
                    "success", isSuccess,
                    "transactionRef", txn.getTransactionRef(),
                    "transactionType", txn.getTransactionType(),
                    "status", txn.getStatus(),
                    "amount", txn.getAmount(),
                    "newBalance", newBalance,
                    "lamportTimestamp", txn.getLamportTimestamp(),
                    "failureReason", txn.getFailureReason() != null ? txn.getFailureReason() : "",
                    "message", isSuccess ? "Transfer completed successfully." : "Transfer failed: " + txn.getFailureReason()
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Transfer processing error: " + e.getMessage()));
        }
    }

    private BigDecimal parseAmount(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Transaction amount is required.");
        }
        String str = obj.toString().trim();
        if (str.isEmpty()) {
            throw new IllegalArgumentException("Transaction amount cannot be empty.");
        }
        try {
            BigDecimal amt = new BigDecimal(str);
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Transaction amount must be positive and greater than zero.");
            }
            return amt;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format '" + str + "'. Please enter a valid numeric value.");
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                            @RequestParam String accountNumber) {
        try {
            User user = authenticateCustomer(authHeader);
            verifyAccountOwnership(user, accountNumber);

            List<Transaction> txns = transactionService.getTransactionHistory(accountNumber);
            return ResponseEntity.ok(txns);
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private void verifyAccountOwnership(User user, String accountNumber) {
        if ("ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            return;
        }
        var accounts = accountService.getAccountsByUserId(user.getId());
        boolean owns = accounts.stream().anyMatch(a -> a.getAccountNumber().equalsIgnoreCase(accountNumber));
        if (!owns) {
            throw new SecurityException("Unauthorized access to account: " + accountNumber);
        }
    }
}
