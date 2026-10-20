package com.distributed.banking.rmi.dto;

import java.io.Serializable;

public class ReplicationResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private Long targetBranchId;
    private Long acknowledgedSequence;
    private String message;
    private long lamportTimestamp;

    public ReplicationResponse() {}

    public ReplicationResponse(boolean success, Long targetBranchId, Long acknowledgedSequence, String message, long lamportTimestamp) {
        this.success = success;
        this.targetBranchId = targetBranchId;
        this.acknowledgedSequence = acknowledgedSequence;
        this.message = message;
        this.lamportTimestamp = lamportTimestamp;
    }

    public boolean isSuccess() { return success; }
    public Long getTargetBranchId() { return targetBranchId; }
    public Long getAcknowledgedSequence() { return acknowledgedSequence; }
    public String getMessage() { return message; }
    public long getLamportTimestamp() { return lamportTimestamp; }
}
