package com.distributed.banking.rmi;

import com.distributed.banking.distributed.LamportClock;
import com.distributed.banking.model.*;
import com.distributed.banking.repository.*;
import com.distributed.banking.rmi.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Authentic RMI implementation for an individual bank branch node.
 * Uses programmatic TransactionTemplate to ensure database transaction boundaries
 * are guaranteed across remote RMI thread pools.
 */
public class BranchServiceImpl extends UnicastRemoteObject implements BranchServiceRemote {

    private static final Logger log = LoggerFactory.getLogger(BranchServiceImpl.class);

    private final Long nodeId;
    private final String nodeName;
    private final LamportClock lamportClock;
    private final AccountRepository accountRepository;
    private final PreparedReservationRepository reservationRepository;
    private final TransactionRepository transactionRepository;
    private final LamportEventRepository eventRepository;
    private final ReplicationRecordRepository replicationRepository;
    private final BranchRepository branchRepository;
    private final TransactionTemplate transactionTemplate;

    private volatile boolean simulatedFailure = false;

    public BranchServiceImpl(Long nodeId, String nodeName, LamportClock lamportClock,
                             AccountRepository accountRepository,
                             PreparedReservationRepository reservationRepository,
                             TransactionRepository transactionRepository,
                             LamportEventRepository eventRepository,
                             ReplicationRecordRepository replicationRepository,
                             BranchRepository branchRepository,
                             PlatformTransactionManager transactionManager) throws RemoteException {
        super(0);
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.lamportClock = lamportClock;
        this.accountRepository = accountRepository;
        this.reservationRepository = reservationRepository;
        this.transactionRepository = transactionRepository;
        this.eventRepository = eventRepository;
        this.replicationRepository = replicationRepository;
        this.branchRepository = branchRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    private void checkLiveness() throws RemoteException {
        if (simulatedFailure) {
            throw new RemoteException("Branch node '" + nodeName + "' (ID: " + nodeId + ") is UNAVAILABLE due to simulated crash/network failure.");
        }
    }

    @Override
    public PrepareResponse prepareTransaction(PrepareRequest request) throws RemoteException {
        checkLiveness();
        long currentTs = lamportClock.updateAndTick(request.getLamportTimestamp());

        log.info("[2PC-PREPARE] Node {} received prepare for GTX: {}, account: {}, amount: {}, debit: {}, L(e)={}",
                nodeId, request.getGlobalTransactionId(), request.getAccountNumber(), request.getAmount(), request.isDebit(), currentTs);

        return transactionTemplate.execute(status -> {
            if (request.isDebit()) {
                var accOpt = accountRepository.findByAccountNumberForUpdate(request.getAccountNumber());
                if (accOpt.isEmpty()) {
                    logEvent("TXN_PREPARE_REJECT", "Debit prepare rejected: Account " + request.getAccountNumber() + " not found");
                    return new PrepareResponse(request.getGlobalTransactionId(), "VOTE_ABORT",
                            "Account " + request.getAccountNumber() + " not found at branch " + nodeName, lamportClock.tick());
                }

                Account account = accOpt.get();
                if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
                    logEvent("TXN_PREPARE_REJECT", "Debit prepare rejected: Account is " + account.getStatus());
                    return new PrepareResponse(request.getGlobalTransactionId(), "VOTE_ABORT",
                            "Account " + request.getAccountNumber() + " is " + account.getStatus(), lamportClock.tick());
                }

                List<PreparedReservation> activeReservations = reservationRepository
                        .findByAccountNumberAndStatus(request.getAccountNumber(), "PREPARED");
                BigDecimal reservedTotal = activeReservations.stream()
                        .filter(r -> "DEBIT_RESERVATION".equalsIgnoreCase(r.getReservationType()))
                        .map(PreparedReservation::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal available = account.getBalance().subtract(reservedTotal);
                if (available.compareTo(request.getAmount()) < 0) {
                    logEvent("TXN_PREPARE_REJECT", "Debit prepare rejected: Insufficient funds. Balance: "
                            + account.getBalance() + ", Reserved: " + reservedTotal + ", Requested: " + request.getAmount());
                    return new PrepareResponse(request.getGlobalTransactionId(), "VOTE_ABORT",
                            "Insufficient funds in " + request.getAccountNumber() + " (Available: " + available + ")", lamportClock.tick());
                }

                PreparedReservation reservation = new PreparedReservation(
                        request.getGlobalTransactionId(),
                        request.getAccountNumber(),
                        "DEBIT_RESERVATION",
                        request.getAmount(),
                        "PREPARED",
                        LocalDateTime.now().plusMinutes(5)
                );
                reservationRepository.save(reservation);

                logEvent("TXN_PREPARE", "Debit prepared: Reserved " + request.getAmount() + " on " + request.getAccountNumber() + " for GTX " + request.getGlobalTransactionId());
                return new PrepareResponse(request.getGlobalTransactionId(), "VOTE_COMMIT",
                        "Funds reserved successfully", lamportClock.tick());

            } else {
                var accOpt = accountRepository.findByAccountNumber(request.getAccountNumber());
                if (accOpt.isEmpty()) {
                    logEvent("TXN_PREPARE_REJECT", "Credit prepare rejected: Target account " + request.getAccountNumber() + " not found");
                    return new PrepareResponse(request.getGlobalTransactionId(), "VOTE_ABORT",
                            "Destination account " + request.getAccountNumber() + " does not exist at branch " + nodeName, lamportClock.tick());
                }

                Account account = accOpt.get();
                if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
                    logEvent("TXN_PREPARE_REJECT", "Credit prepare rejected: Target account is " + account.getStatus());
                    return new PrepareResponse(request.getGlobalTransactionId(), "VOTE_ABORT",
                            "Destination account " + request.getAccountNumber() + " is " + account.getStatus(), lamportClock.tick());
                }

                PreparedReservation reservation = new PreparedReservation(
                        request.getGlobalTransactionId(),
                        request.getAccountNumber(),
                        "CREDIT_RESERVATION",
                        request.getAmount(),
                        "PREPARED",
                        LocalDateTime.now().plusMinutes(5)
                );
                reservationRepository.save(reservation);

                logEvent("TXN_PREPARE", "Credit prepared: Verified " + request.getAccountNumber() + " for GTX " + request.getGlobalTransactionId());
                return new PrepareResponse(request.getGlobalTransactionId(), "VOTE_COMMIT",
                        "Destination account verified and prepared", lamportClock.tick());
            }
        });
    }

    @Override
    public CommitResponse commitTransaction(CommitRequest request) throws RemoteException {
        checkLiveness();
        long currentTs = lamportClock.updateAndTick(request.getLamportTimestamp());

        log.info("[2PC-PHASE2] Node {} received decision: {} for GTX: {}, L(e)={}",
                nodeId, request.getDecision(), request.getGlobalTransactionId(), currentTs);

        return transactionTemplate.execute(status -> {
            List<PreparedReservation> reservations = reservationRepository
                    .findByGlobalTransactionIdAndStatus(request.getGlobalTransactionId(), "PREPARED");

            if (request.isGlobalCommit()) {
                for (PreparedReservation res : reservations) {
                    var accOpt = accountRepository.findByAccountNumberForUpdate(res.getAccountNumber());
                    if (accOpt.isPresent()) {
                        Account account = accOpt.get();
                        if ("DEBIT_RESERVATION".equalsIgnoreCase(res.getReservationType())) {
                            account.setBalance(account.getBalance().subtract(res.getAmount()));
                            log.info("Node {} committed DEBIT of {} from {}", nodeId, res.getAmount(), account.getAccountNumber());
                        } else if ("CREDIT_RESERVATION".equalsIgnoreCase(res.getReservationType())) {
                            account.setBalance(account.getBalance().add(res.getAmount()));
                            log.info("Node {} committed CREDIT of {} to {}", nodeId, res.getAmount(), account.getAccountNumber());
                        }
                        accountRepository.save(account);
                    }
                    res.setStatus("COMMITTED");
                    reservationRepository.save(res);
                }

                logEvent("TXN_COMMIT", "Phase 2 COMMIT executed for GTX: " + request.getGlobalTransactionId());
                return new CommitResponse(request.getGlobalTransactionId(), true, "Committed successfully", lamportClock.tick());

            } else {
                for (PreparedReservation res : reservations) {
                    res.setStatus("RELEASED");
                    reservationRepository.save(res);
                }

                logEvent("TXN_ABORT", "Phase 2 ABORT executed for GTX: " + request.getGlobalTransactionId() + ". Reservations released.");
                return new CommitResponse(request.getGlobalTransactionId(), true, "Aborted and locks released", lamportClock.tick());
            }
        });
    }

    @Override
    public ReplicationResponse receiveReplication(ReplicationRequest request) throws RemoteException {
        checkLiveness();
        long currentTs = lamportClock.updateAndTick(request.getLamportTimestamp());

        return transactionTemplate.execute(status -> {
            ReplicationRecord record = new ReplicationRecord(
                    request.getSequenceNumber(),
                    request.getSourceBranchId(),
                    request.getTargetBranchId(),
                    request.getTransactionRef(),
                    request.getAccountNumber(),
                    request.getChangeType(),
                    request.getAmount(),
                    "SYNCED",
                    currentTs
            );
            replicationRepository.save(record);

            logEvent("REPLICA_SYNC", "Received replica audit record seq=" + request.getSequenceNumber()
                    + " from Branch " + request.getSourceBranchId() + " for account " + request.getAccountNumber());

            return new ReplicationResponse(true, nodeId, request.getSequenceNumber(), "Replication persisted", lamportClock.tick());
        });
    }

    @Override
    public HeartbeatMessage ping(HeartbeatMessage msg) throws RemoteException {
        checkLiveness();
        lamportClock.updateAndTick(msg.getLamportTimestamp());
        return new HeartbeatMessage(nodeId, nodeName, false, "ONLINE", lamportClock.tick());
    }

    @Override
    public ElectionMessage handleElection(ElectionMessage msg) throws RemoteException {
        checkLiveness();
        long currentTs = lamportClock.updateAndTick(msg.getLamportTimestamp());

        log.info("Node {} received ElectionMessage type={} from sender {} (L={})",
                nodeId, msg.getType(), msg.getSenderId(), currentTs);

        logEvent("ELECTION_MSG", "Received " + msg.getType() + " from Node " + msg.getSenderId());

        if (msg.getType() == ElectionMessage.Type.ELECTION) {
            if (this.nodeId > msg.getSenderId()) {
                return new ElectionMessage(nodeId, nodeName, ElectionMessage.Type.OK, lamportClock.tick());
            }
        }
        return new ElectionMessage(nodeId, nodeName, ElectionMessage.Type.OK, lamportClock.tick());
    }

    @Override
    public BigDecimal getAccountBalance(String accountNumber, long clientLamportTime) throws RemoteException {
        checkLiveness();
        lamportClock.updateAndTick(clientLamportTime);

        return accountRepository.findByAccountNumber(accountNumber)
                .map(Account::getBalance)
                .orElseThrow(() -> new RemoteException("Account " + accountNumber + " not found at branch " + nodeName));
    }

    @Override
    public boolean isNodeAlive() throws RemoteException {
        return !simulatedFailure;
    }

    @Override
    public void simulateFailure(boolean fail) throws RemoteException {
        this.simulatedFailure = fail;
        String status = fail ? "UNAVAILABLE" : "ONLINE";
        branchRepository.findById(nodeId).ifPresent(b -> {
            b.setStatus(status);
            branchRepository.save(b);
        });

        long ts = lamportClock.tick();
        logEvent(fail ? "NODE_FAILURE" : "NODE_RECOVERY",
                "Branch node " + nodeName + " (ID: " + nodeId + ") marked as " + status + " (simulated)");
        log.warn("Node {} simulated failure set to {}", nodeId, fail);
    }

    private void logEvent(String eventType, String message) {
        try {
            LamportEvent event = new LamportEvent(nodeId, nodeName, lamportClock.getTime(), eventType, message);
            eventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to persist Lamport event: {}", e.getMessage());
        }
    }

    @Override
    public Long getNodeId() { return nodeId; }

    @Override
    public String getNodeName() { return nodeName; }
}
