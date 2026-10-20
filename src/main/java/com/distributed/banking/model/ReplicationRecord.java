package com.distributed.banking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "replication_audit_log")
public class ReplicationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Column(name = "source_branch_id", nullable = false)
    private Long sourceBranchId;

    @Column(name = "target_branch_id", nullable = false)
    private Long targetBranchId;

    @Column(name = "transaction_ref", nullable = false, length = 64)
    private String transactionRef;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "change_type", nullable = false, length = 30)
    private String changeType; // BALANCE_UPDATE, AUDIT_RECORD

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "sync_status", nullable = false, length = 30)
    private String syncStatus; // SYNCED, PENDING_RECONCILIATION, STALE

    @Column(name = "lamport_timestamp", nullable = false)
    private Long lamportTimestamp;

    @Column(name = "synced_at", insertable = false, updatable = false)
    private LocalDateTime syncedAt;

    public ReplicationRecord() {}

    public ReplicationRecord(Long sequenceNumber, Long sourceBranchId, Long targetBranchId,
                             String transactionRef, String accountNumber, String changeType,
                             BigDecimal amount, String syncStatus, Long lamportTimestamp) {
        this.sequenceNumber = sequenceNumber;
        this.sourceBranchId = sourceBranchId;
        this.targetBranchId = targetBranchId;
        this.transactionRef = transactionRef;
        this.accountNumber = accountNumber;
        this.changeType = changeType;
        this.amount = amount;
        this.syncStatus = syncStatus;
        this.lamportTimestamp = lamportTimestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public Long getSourceBranchId() { return sourceBranchId; }
    public void setSourceBranchId(Long sourceBranchId) { this.sourceBranchId = sourceBranchId; }

    public Long getTargetBranchId() { return targetBranchId; }
    public void setTargetBranchId(Long targetBranchId) { this.targetBranchId = targetBranchId; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public Long getLamportTimestamp() { return lamportTimestamp; }
    public void setLamportTimestamp(Long lamportTimestamp) { this.lamportTimestamp = lamportTimestamp; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }
}
