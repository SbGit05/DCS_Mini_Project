package com.distributed.banking.rmi.dto;

import java.io.Serializable;

public class CommitRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String globalTransactionId;
    private String decision; // GLOBAL_COMMIT or GLOBAL_ABORT
    private long lamportTimestamp;

    public CommitRequest() {}

    public CommitRequest(String globalTransactionId, String decision, long lamportTimestamp) {
        this.globalTransactionId = globalTransactionId;
        this.decision = decision;
        this.lamportTimestamp = lamportTimestamp;
    }

    public String getGlobalTransactionId() { return globalTransactionId; }
    public String getDecision() { return decision; }
    public long getLamportTimestamp() { return lamportTimestamp; }
    public boolean isGlobalCommit() { return "GLOBAL_COMMIT".equalsIgnoreCase(decision); }
}
