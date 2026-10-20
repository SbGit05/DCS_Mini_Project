package com.distributed.banking.repository;

import com.distributed.banking.model.TwoPhaseTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TwoPhaseTransactionRepository extends JpaRepository<TwoPhaseTransaction, Long> {
    Optional<TwoPhaseTransaction> findByGlobalTransactionId(String globalTransactionId);
    List<TwoPhaseTransaction> findTop50ByOrderByCreatedAtDesc();
    List<TwoPhaseTransaction> findByState(String state);
}
