package com.distributed.banking.service;

import com.distributed.banking.distributed.BullyElectionService;
import com.distributed.banking.distributed.LamportClock;
import com.distributed.banking.distributed.ReplicationManager;
import com.distributed.banking.distributed.TwoPhaseCommitCoordinator;
import com.distributed.banking.model.*;
import com.distributed.banking.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditService {

    private final BranchRepository branchRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TwoPhaseTransactionRepository twoPhaseRepository;
    private final LamportEventRepository eventRepository;
    private final ReplicationRecordRepository replicationRepository;
    private final LamportClock lamportClock;
    private final BullyElectionService electionService;
    private final TwoPhaseCommitCoordinator coordinator;
    private final ReplicationManager replicationManager;

    public AuditService(BranchRepository branchRepository,
                        AccountRepository accountRepository,
                        TransactionRepository transactionRepository,
                        TwoPhaseTransactionRepository twoPhaseRepository,
                        LamportEventRepository eventRepository,
                        ReplicationRecordRepository replicationRepository,
                        LamportClock lamportClock,
                        BullyElectionService electionService,
                        TwoPhaseCommitCoordinator coordinator,
                        ReplicationManager replicationManager) {
        this.branchRepository = branchRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.twoPhaseRepository = twoPhaseRepository;
        this.eventRepository = eventRepository;
        this.replicationRepository = replicationRepository;
        this.lamportClock = lamportClock;
        this.electionService = electionService;
        this.coordinator = coordinator;
        this.replicationManager = replicationManager;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSystemDashboard() {
        Map<String, Object> stats = new LinkedHashMap<>();

        List<Branch> branches = branchRepository.findAllByOrderByIdAsc();
        List<Account> accounts = accountRepository.findAll();
        List<Transaction> transactions = transactionRepository.findTop50ByOrderByCreatedAtDesc();
        List<TwoPhaseTransaction> twoPhaseTxns = twoPhaseRepository.findTop50ByOrderByCreatedAtDesc();
        List<LamportEvent> events = eventRepository.findTop100ByOrderByLogicalTimestampDesc();

        long onlineNodes = branches.stream().filter(b -> "ONLINE".equalsIgnoreCase(b.getStatus())).count();
        BigDecimal totalDeposits = accounts.stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("totalBranches", branches.size());
        stats.put("onlineBranches", onlineNodes);
        stats.put("totalAccounts", accounts.size());
        stats.put("totalSystemBalance", totalDeposits);
        stats.put("totalTransactions", transactions.size());
        stats.put("currentLamportClock", lamportClock.getTime());
        stats.put("activeCoordinatorId", coordinator.getActiveCoordinatorId());
        stats.put("isCoordinatorFailing", coordinator.isSimulatedFailure());

        stats.put("branches", branches);
        stats.put("accounts", accounts);
        stats.put("recentTransactions", transactions);
        stats.put("twoPhaseTransactions", twoPhaseTxns);
        stats.put("lamportEvents", events);
        stats.put("replicationDiscrepancies", replicationManager.checkDiscrepancies());

        return stats;
    }

    @Transactional(readOnly = true)
    public List<LamportEvent> getLamportEvents() {
        return eventRepository.findTop100ByOrderByLogicalTimestampDesc();
    }

    @Transactional(readOnly = true)
    public List<TwoPhaseTransaction> getTwoPhaseTransactions() {
        return twoPhaseRepository.findTop50ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<ReplicationRecord> getReplicationAuditRecords() {
        return replicationRepository.findTop50ByOrderBySequenceNumberDesc();
    }
}
