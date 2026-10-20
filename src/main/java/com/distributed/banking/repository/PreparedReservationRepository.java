package com.distributed.banking.repository;

import com.distributed.banking.model.PreparedReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreparedReservationRepository extends JpaRepository<PreparedReservation, Long> {
    Optional<PreparedReservation> findByGlobalTransactionId(String globalTransactionId);
    List<PreparedReservation> findByAccountNumberAndStatus(String accountNumber, String status);
    List<PreparedReservation> findByGlobalTransactionIdAndStatus(String globalTransactionId, String status);
}
