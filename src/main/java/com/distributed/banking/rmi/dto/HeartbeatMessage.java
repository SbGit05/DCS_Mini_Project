package com.distributed.banking.rmi.dto;

import java.io.Serializable;

public class HeartbeatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long nodeId;
    private String nodeName;
    private boolean coordinator;
    private String status; // ONLINE, UNAVAILABLE, RECOVERING
    private long lamportTimestamp;

    public HeartbeatMessage() {}

    public HeartbeatMessage(Long nodeId, String nodeName, boolean coordinator, String status, long lamportTimestamp) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.coordinator = coordinator;
        this.status = status;
        this.lamportTimestamp = lamportTimestamp;
    }

    public Long getNodeId() { return nodeId; }
    public String getNodeName() { return nodeName; }
    public boolean isCoordinator() { return coordinator; }
    public String getStatus() { return status; }
    public long getLamportTimestamp() { return lamportTimestamp; }
}
