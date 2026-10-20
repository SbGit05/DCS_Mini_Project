package com.distributed.banking.service;

import com.distributed.banking.model.Account;
import com.distributed.banking.model.Branch;
import com.distributed.banking.model.User;
import com.distributed.banking.repository.AccountRepository;
import com.distributed.banking.repository.BranchRepository;
import com.distributed.banking.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BranchRepository branchRepository;

    // In-memory active session tokens: token -> User
    private final Map<String, User> activeSessions = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository,
                       AccountRepository accountRepository,
                       BranchRepository branchRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.branchRepository = branchRepository;
    }

    @Transactional
    public User registerCustomer(String username, String password, String fullName, String email, Long branchId) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered.");
        }
        if (password == null || password.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Selected branch does not exist."));

        String hash = BCrypt.hashpw(password, BCrypt.gensalt(10));
        User user = new User(username, hash, fullName, email, "ROLE_CUSTOMER");
        user = userRepository.save(user);

        // Auto-create initial savings account at the branch
        String prefix = branch.getBranchCode().substring(0, 3).toUpperCase();
        String accNum = prefix + "-" + (1000 + user.getId() * 10);
        Account account = new Account(accNum, user, branch, "SAVINGS", BigDecimal.valueOf(1000.00), "ACTIVE");
        accountRepository.save(account);

        return user;
    }

    public Map<String, Object> login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        User user = userOpt.get();
        boolean passwordMatch = false;

        try {
            // Support both standard $2a and $2b BCrypt prefixes
            String hash = user.getPasswordHash();
            if (hash.startsWith("$2b$")) {
                hash = "$2a$" + hash.substring(4);
            }
            passwordMatch = BCrypt.checkpw(password, hash);
        } catch (Exception e) {
            passwordMatch = false;
        }

        if (!passwordMatch) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        String token = UUID.randomUUID().toString();
        activeSessions.put(token, user);

        // Look up primary account if customer
        String accountNumber = null;
        BigDecimal balance = BigDecimal.ZERO;
        String branchName = null;

        var accounts = accountRepository.findByUserId(user.getId());
        if (!accounts.isEmpty()) {
            Account acc = accounts.get(0);
            accountNumber = acc.getAccountNumber();
            balance = acc.getBalance();
            branchName = acc.getBranch().getBranchName();
        }

        return Map.of(
                "token", token,
                "userId", user.getId(),
                "username", user.getUsername(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "accountNumber", accountNumber != null ? accountNumber : "",
                "balance", balance,
                "branchName", branchName != null ? branchName : ""
        );
    }

    public User validateToken(String token) {
        if (token == null || !activeSessions.containsKey(token)) {
            throw new SecurityException("Unauthorized: Session is invalid or expired.");
        }
        return activeSessions.get(token);
    }

    public void logout(String token) {
        if (token != null) {
            activeSessions.remove(token);
        }
    }
}
