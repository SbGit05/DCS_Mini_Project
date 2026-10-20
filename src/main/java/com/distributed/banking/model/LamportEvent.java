package com.distributed.banking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lamport_event_log")
public class LamportEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    @Column(name = "node_name", nullable = false, length = 50)
    private String nodeName;

    @Column(name = "logical_timestamp", nullable = false)
    private Long logicalTimestamp;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public LamportEvent() {}

    public LamportEvent(Long nodeId, String nodeName, Long logicalTimestamp, String eventType, String message) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.logicalTimestamp = logicalTimestamp;
        this.eventType = eventType;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNodeId() { return nodeId; }
    public void setNodeId(Long nodeId) { this.nodeId = nodeId; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public Long getLogicalTimestamp() { return logicalTimestamp; }
    public void setLogicalTimestamp(Long logicalTimestamp) { this.logicalTimestamp = logicalTimestamp; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
