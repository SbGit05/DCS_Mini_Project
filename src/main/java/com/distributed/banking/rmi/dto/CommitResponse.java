package com.distributed.banking.rmi.dto;

import java.io.Serializable;

public class CommitResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String globalTransactionId;
    private boolean success;
    private String message;
    private long lamportTimestamp;

    public CommitResponse() {}

    public CommitResponse(String globalTransactionId, boolean success, String message, long lamportTimestamp) {
        this.globalTransactionId = globalTransactionId;
        this.success = success;
        this.message = message;
        this.lamportTimestamp = lamportTimestamp;
    }

    public String getGlobalTransactionId() { return globalTransactionId; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public long getLamportTimestamp() { return lamportTimestamp; }
}
