package com.distributed.banking.rmi.dto;

import java.io.Serializable;

public class ElectionMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        ELECTION,
        OK,
        COORDINATOR
    }

    private Long senderId;
    private String senderName;
    private Type type;
    private long lamportTimestamp;

    public ElectionMessage() {}

    public ElectionMessage(Long senderId, String senderName, Type type, long lamportTimestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.type = type;
        this.lamportTimestamp = lamportTimestamp;
    }

    public Long getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public Type getType() { return type; }
    public long getLamportTimestamp() { return lamportTimestamp; }
}
