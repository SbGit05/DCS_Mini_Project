package com.distributed.banking.rmi.dto;

import java.io.Serializable;

public class PrepareResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String globalTransactionId;
    private String vote; // VOTE_COMMIT or VOTE_ABORT
    private String reason;
    private long lamportTimestamp;

    public PrepareResponse() {}

    public PrepareResponse(String globalTransactionId, String vote, String reason, long lamportTimestamp) {
        this.globalTransactionId = globalTransactionId;
        this.vote = vote;
        this.reason = reason;
        this.lamportTimestamp = lamportTimestamp;
    }

    public String getGlobalTransactionId() { return globalTransactionId; }
    public String getVote() { return vote; }
    public String getReason() { return reason; }
    public long getLamportTimestamp() { return lamportTimestamp; }
    public boolean isCommitVote() { return "VOTE_COMMIT".equalsIgnoreCase(vote); }
}
