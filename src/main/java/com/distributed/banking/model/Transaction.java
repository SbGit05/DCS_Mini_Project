package com.distributed.banking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 64)
    private String transactionRef;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "source_account_number", length = 30)
    private String sourceAccountNumber;

    @Column(name = "destination_account_number", length = 30)
    private String destinationAccountNumber;

    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType; // DEPOSIT, WITHDRAWAL, TRANSFER_LOCAL, TRANSFER_DISTRIBUTED

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "origin_branch_id", nullable = false)
    private Long originBranchId;

    @Column(name = "destination_branch_id")
    private Long destinationBranchId;

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS, FAILED, PENDING, ABORTED

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "lamport_timestamp", nullable = false)
    private Long lamportTimestamp;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Transaction() {}

    public Transaction(String transactionRef, String idempotencyKey, String sourceAccountNumber,
                       String destinationAccountNumber, String transactionType, BigDecimal amount,
                       Long originBranchId, Long destinationBranchId, String status,
                       String failureReason, Long lamportTimestamp) {
        this.transactionRef = transactionRef;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.originBranchId = originBranchId;
        this.destinationBranchId = destinationBranchId;
        this.status = status;
        this.failureReason = failureReason;
        this.lamportTimestamp = lamportTimestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }

    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public void setDestinationAccountNumber(String destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Long getOriginBranchId() { return originBranchId; }
    public void setOriginBranchId(Long originBranchId) { this.originBranchId = originBranchId; }

    public Long getDestinationBranchId() { return destinationBranchId; }
    public void setDestinationBranchId(Long destinationBranchId) { this.destinationBranchId = destinationBranchId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Long getLamportTimestamp() { return lamportTimestamp; }
    public void setLamportTimestamp(Long lamportTimestamp) { this.lamportTimestamp = lamportTimestamp; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
