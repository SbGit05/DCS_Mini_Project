package com.distributed.banking.rmi;

import com.distributed.banking.model.TwoPhaseTransaction;
import java.math.BigDecimal;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI Remote Interface for the Distributed Transaction Coordinator (2PC).
 */
public interface CoordinatorServiceRemote extends Remote {

    /**
     * Executes a cross-branch distributed fund transfer using Two-Phase Commit protocol.
     */
    TwoPhaseTransaction executeDistributedTransfer(String fromAccount, String toAccount,
                                                  BigDecimal amount, String idempotencyKey,
                                                  long clientLamportTime) throws RemoteException;

    /**
     * Triggers a Bully election among coordinator nodes.
     */
    void initiateElection(long initiatorNodeId) throws RemoteException;

    /**
     * Returns the active coordinator node ID.
     */
    Long getActiveCoordinatorId() throws RemoteException;

    boolean isCoordinatorAlive() throws RemoteException;
}
