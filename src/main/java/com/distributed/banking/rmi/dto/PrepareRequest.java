package com.distributed.banking.rmi.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class PrepareRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String globalTransactionId;
    private String accountNumber;
    private BigDecimal amount;
    private boolean debit;
    private long lamportTimestamp;

    public PrepareRequest() {}

    public PrepareRequest(String globalTransactionId, String accountNumber, BigDecimal amount, boolean debit, long lamportTimestamp) {
        this.globalTransactionId = globalTransactionId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.debit = debit;
        this.lamportTimestamp = lamportTimestamp;
    }

    public String getGlobalTransactionId() { return globalTransactionId; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getAmount() { return amount; }
    public boolean isDebit() { return debit; }
    public long getLamportTimestamp() { return lamportTimestamp; }
}
