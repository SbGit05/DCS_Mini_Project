package com.distributed.banking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "branches")
public class Branch {

    @Id
    private Long id; // Explicit node numeric ID (e.g. 1, 2, 3)

    @Column(name = "branch_code", nullable = false, unique = true, length = 10)
    private String branchCode;

    @Column(name = "branch_name", nullable = false, length = 100)
    private String branchName;

    @Column(nullable = false, length = 100)
    private String location;

    @Column(name = "rmi_host", nullable = false, length = 100)
    private String rmiHost;

    @Column(name = "rmi_port", nullable = false)
    private Integer rmiPort;

    @Column(name = "rmi_service_name", nullable = false, length = 100)
    private String rmiServiceName;

    @Column(nullable = false, length = 20)
    private String status; // ONLINE, UNAVAILABLE, RECOVERING

    @Column(name = "is_coordinator", nullable = false)
    private Boolean isCoordinator;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    public Branch() {}

    public Branch(Long id, String branchCode, String branchName, String location, String rmiHost, Integer rmiPort, String rmiServiceName, String status, Boolean isCoordinator) {
        this.id = id;
        this.branchCode = branchCode;
        this.branchName = branchName;
        this.location = location;
        this.rmiHost = rmiHost;
        this.rmiPort = rmiPort;
        this.rmiServiceName = rmiServiceName;
        this.status = status;
        this.isCoordinator = isCoordinator;
        this.lastHeartbeat = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getRmiHost() { return rmiHost; }
    public void setRmiHost(String rmiHost) { this.rmiHost = rmiHost; }

    public Integer getRmiPort() { return rmiPort; }
    public void setRmiPort(Integer rmiPort) { this.rmiPort = rmiPort; }

    public String getRmiServiceName() { return rmiServiceName; }
    public void setRmiServiceName(String rmiServiceName) { this.rmiServiceName = rmiServiceName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsCoordinator() { return isCoordinator; }
    public void setIsCoordinator(Boolean coordinator) { isCoordinator = coordinator; }

    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
}
