package com.distributed.banking.config;

import com.distributed.banking.distributed.*;
import com.distributed.banking.repository.*;
import com.distributed.banking.rmi.BranchServiceImpl;
import com.distributed.banking.rmi.BranchServiceRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Spring Configuration to bootstrap the Java RMI Registry and export
 * independent remote branch service nodes.
 */
@Configuration
public class RmiConfig {

    private static final Logger log = LoggerFactory.getLogger(RmiConfig.class);

    @Value("${bank.rmi.registry.port:1099}")
    private int rmiPort;

    @Bean
    public Registry rmiRegistry() {
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(rmiPort);
            log.info("RMI Registry successfully created on port {}", rmiPort);
        } catch (Exception e) {
            try {
                registry = LocateRegistry.getRegistry(rmiPort);
                log.info("Connected to existing RMI Registry on port {}", rmiPort);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to initialize or locate RMI Registry on port " + rmiPort, ex);
            }
        }
        return registry;
    }

    @Bean
    public BranchServiceRemote branchService1(Registry rmiRegistry,
                                             LamportClock lamportClock,
                                             AccountRepository accountRepository,
                                             PreparedReservationRepository reservationRepository,
                                             TransactionRepository transactionRepository,
                                             LamportEventRepository eventRepository,
                                             ReplicationRecordRepository replicationRepository,
                                             BranchRepository branchRepository,
                                             TwoPhaseCommitCoordinator coordinator,
                                             ReplicationManager replicationManager,
                                             BullyElectionService electionService,
                                             NodeFailureDetector failureDetector,
                                             PlatformTransactionManager transactionManager) throws Exception {
        BranchServiceImpl service = new BranchServiceImpl(1L, "Pune Central Branch",
                lamportClock, accountRepository, reservationRepository,
                transactionRepository, eventRepository, replicationRepository, branchRepository, transactionManager);

        rmiRegistry.rebind("BranchService_1", service);
        coordinator.registerBranchStub(1L, service);
        replicationManager.registerStub(1L, service);
        electionService.registerStub(1L, service);
        failureDetector.registerStub(1L, service);

        log.info("Exported and bound 'BranchService_1' (Node 1: Pune Central) to RMI Registry.");
        return service;
    }

    @Bean
    public BranchServiceRemote branchService2(Registry rmiRegistry,
                                             LamportClock lamportClock,
                                             AccountRepository accountRepository,
                                             PreparedReservationRepository reservationRepository,
                                             TransactionRepository transactionRepository,
                                             LamportEventRepository eventRepository,
                                             ReplicationRecordRepository replicationRepository,
                                             BranchRepository branchRepository,
                                             TwoPhaseCommitCoordinator coordinator,
                                             ReplicationManager replicationManager,
                                             BullyElectionService electionService,
                                             NodeFailureDetector failureDetector,
                                             PlatformTransactionManager transactionManager) throws Exception {
        BranchServiceImpl service = new BranchServiceImpl(2L, "Mumbai Main Branch",
                lamportClock, accountRepository, reservationRepository,
                transactionRepository, eventRepository, replicationRepository, branchRepository, transactionManager);

        rmiRegistry.rebind("BranchService_2", service);
        coordinator.registerBranchStub(2L, service);
        replicationManager.registerStub(2L, service);
        electionService.registerStub(2L, service);
        failureDetector.registerStub(2L, service);

        log.info("Exported and bound 'BranchService_2' (Node 2: Mumbai Main) to RMI Registry.");
        return service;
    }

    @Bean
    public BranchServiceRemote branchService3(Registry rmiRegistry,
                                             LamportClock lamportClock,
                                             AccountRepository accountRepository,
                                             PreparedReservationRepository reservationRepository,
                                             TransactionRepository transactionRepository,
                                             LamportEventRepository eventRepository,
                                             ReplicationRecordRepository replicationRepository,
                                             BranchRepository branchRepository,
                                             TwoPhaseCommitCoordinator coordinator,
                                             ReplicationManager replicationManager,
                                             BullyElectionService electionService,
                                             NodeFailureDetector failureDetector,
                                             PlatformTransactionManager transactionManager) throws Exception {
        BranchServiceImpl service = new BranchServiceImpl(3L, "Delhi Regional Hub",
                lamportClock, accountRepository, reservationRepository,
                transactionRepository, eventRepository, replicationRepository, branchRepository, transactionManager);

        rmiRegistry.rebind("BranchService_3", service);
        coordinator.registerBranchStub(3L, service);
        replicationManager.registerStub(3L, service);
        electionService.registerStub(3L, service);
        failureDetector.registerStub(3L, service);

        log.info("Exported and bound 'BranchService_3' (Node 3: Delhi Regional Hub) to RMI Registry.");
        return service;
    }
}
