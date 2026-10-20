package com.distributed.banking.rmi.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class ReplicationRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long sourceBranchId;
    private Long targetBranchId;
    private Long sequenceNumber;
    private String transactionRef;
    private String accountNumber;
    private String changeType; // BALANCE_UPDATE, AUDIT_RECORD
    private BigDecimal amount;
    private long lamportTimestamp;

    public ReplicationRequest() {}

    public ReplicationRequest(Long sourceBranchId, Long targetBranchId, Long sequenceNumber,
                              String transactionRef, String accountNumber, String changeType,
                              BigDecimal amount, long lamportTimestamp) {
        this.sourceBranchId = sourceBranchId;
        this.targetBranchId = targetBranchId;
        this.sequenceNumber = sequenceNumber;
        this.transactionRef = transactionRef;
        this.accountNumber = accountNumber;
        this.changeType = changeType;
        this.amount = amount;
        this.lamportTimestamp = lamportTimestamp;
    }

    public Long getSourceBranchId() { return sourceBranchId; }
    public Long getTargetBranchId() { return targetBranchId; }
    public Long getSequenceNumber() { return sequenceNumber; }
    public String getTransactionRef() { return transactionRef; }
    public String getAccountNumber() { return accountNumber; }
    public String getChangeType() { return changeType; }
    public BigDecimal getAmount() { return amount; }
    public long getLamportTimestamp() { return lamportTimestamp; }
}
