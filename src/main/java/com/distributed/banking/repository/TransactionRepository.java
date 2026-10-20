package com.distributed.banking.repository;

import com.distributed.banking.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionRef(String transactionRef);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM Transaction t WHERE t.sourceAccountNumber = :acc OR t.destinationAccountNumber = :acc ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountNumber(@Param("acc") String accountNumber);

    List<Transaction> findTop50ByOrderByCreatedAtDesc();

    List<Transaction> findByOriginBranchIdOrderByCreatedAtDesc(Long branchId);
}
