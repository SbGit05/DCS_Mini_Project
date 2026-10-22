package com.distributed.banking.rmi;

import com.distributed.banking.rmi.dto.*;

import java.math.BigDecimal;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI Remote Interface implemented by each independent branch node
 * (e.g., Pune Central Branch, Mumbai Main Branch, Delhi Hub).
 */
public interface BranchServiceRemote extends Remote {

    /**
     * Phase 1 of Two-Phase Commit (2PC): Prepare local reservation.
     */
    PrepareResponse prepareTransaction(PrepareRequest request) throws RemoteException;

    /**
     * Phase 2 of Two-Phase Commit (2PC): Commit or abort the prepared reservation.
     */
    CommitResponse commitTransaction(CommitRequest request) throws RemoteException;

    /**
     * Asynchronous / Semi-synchronous replica update reception.
     */
    ReplicationResponse receiveReplication(ReplicationRequest request) throws RemoteException;

    /**
     * Node heartbeat and health check.
     */
    HeartbeatMessage ping(HeartbeatMessage msg) throws RemoteException;

    /**
     * Bully election message handler.
     */
    ElectionMessage handleElection(ElectionMessage msg) throws RemoteException;

    /**
     * Remote balance inquiry with Lamport clock synchronization.
     */
    BigDecimal getAccountBalance(String accountNumber, long clientLamportTime) throws RemoteException;

    /**
     * Health status probe.
     */
    boolean isNodeAlive() throws RemoteException;

    /**
     * Sets explicit node lifecycle state (RUNNING, STOPPED, CRASHED, RECOVERING).
     */
    void setNodeState(String state) throws RemoteException;

    /**
     * Retrieves the current node lifecycle state.
     */
    String getNodeState() throws RemoteException;

    /**
     * Simulates branch crash / network failure for laboratory demonstration.
     */
    void simulateFailure(boolean fail) throws RemoteException;

    Long getNodeId() throws RemoteException;

    String getNodeName() throws RemoteException;
}
