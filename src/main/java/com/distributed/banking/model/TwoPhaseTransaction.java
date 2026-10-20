package com.distributed.banking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "two_phase_transactions")
public class TwoPhaseTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "global_transaction_id", nullable = false, unique = true, length = 64)
    private String globalTransactionId;

    @Column(name = "source_account", nullable = false, length = 30)
    private String sourceAccount;

    @Column(name = "dest_account", nullable = false, length = 30)
    private String destAccount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "source_branch_id", nullable = false)
    private Long sourceBranchId;

    @Column(name = "dest_branch_id", nullable = false)
    private Long destBranchId;

    @Column(name = "coordinator_node_id", nullable = false)
    private Long coordinatorNodeId;

    @Column(name = "phase1_source_vote", length = 20)
    private String phase1SourceVote; // VOTE_COMMIT, VOTE_ABORT, PENDING, TIMEOUT

    @Column(name = "phase1_dest_vote", length = 20)
    private String phase1DestVote; // VOTE_COMMIT, VOTE_ABORT, PENDING, TIMEOUT

    @Column(length = 20)
    private String decision; // GLOBAL_COMMIT, GLOBAL_ABORT, IN_PROGRESS

    @Column(nullable = false, length = 20)
    private String state; // INITIATED, PREPARED, COMMITTED, ABORTED

    @Column(name = "lamport_timestamp", nullable = false)
    private Long lamportTimestamp;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public TwoPhaseTransaction() {}

    public TwoPhaseTransaction(String globalTransactionId, String sourceAccount, String destAccount,
                               BigDecimal amount, Long sourceBranchId, Long destBranchId,
                               Long coordinatorNodeId, String state, Long lamportTimestamp) {
        this.globalTransactionId = globalTransactionId;
        this.sourceAccount = sourceAccount;
        this.destAccount = destAccount;
        this.amount = amount;
        this.sourceBranchId = sourceBranchId;
        this.destBranchId = destBranchId;
        this.coordinatorNodeId = coordinatorNodeId;
        this.phase1SourceVote = "PENDING";
        this.phase1DestVote = "PENDING";
        this.decision = "IN_PROGRESS";
        this.state = state;
        this.lamportTimestamp = lamportTimestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGlobalTransactionId() { return globalTransactionId; }
    public void setGlobalTransactionId(String globalTransactionId) { this.globalTransactionId = globalTransactionId; }

    public String getSourceAccount() { return sourceAccount; }
    public void setSourceAccount(String sourceAccount) { this.sourceAccount = sourceAccount; }

    public String getDestAccount() { return destAccount; }
    public void setDestAccount(String destAccount) { this.destAccount = destAccount; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Long getSourceBranchId() { return sourceBranchId; }
    public void setSourceBranchId(Long sourceBranchId) { this.sourceBranchId = sourceBranchId; }

    public Long getDestBranchId() { return destBranchId; }
    public void setDestBranchId(Long destBranchId) { this.destBranchId = destBranchId; }

    public Long getCoordinatorNodeId() { return coordinatorNodeId; }
    public void setCoordinatorNodeId(Long coordinatorNodeId) { this.coordinatorNodeId = coordinatorNodeId; }

    public String getPhase1SourceVote() { return phase1SourceVote; }
    public void setPhase1SourceVote(String phase1SourceVote) { this.phase1SourceVote = phase1SourceVote; }

    public String getPhase1DestVote() { return phase1DestVote; }
    public void setPhase1DestVote(String phase1DestVote) { this.phase1DestVote = phase1DestVote; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Long getLamportTimestamp() { return lamportTimestamp; }
    public void setLamportTimestamp(Long lamportTimestamp) { this.lamportTimestamp = lamportTimestamp; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
