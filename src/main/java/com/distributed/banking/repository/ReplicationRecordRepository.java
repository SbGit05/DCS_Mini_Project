package com.distributed.banking.repository;

import com.distributed.banking.model.ReplicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplicationRecordRepository extends JpaRepository<ReplicationRecord, Long> {
    List<ReplicationRecord> findTop50ByOrderBySequenceNumberDesc();
    List<ReplicationRecord> findByTargetBranchIdAndSyncStatus(Long targetBranchId, String syncStatus);
    List<ReplicationRecord> findByTargetBranchIdOrderBySequenceNumberAsc(Long targetBranchId);
    long countBySyncStatus(String syncStatus);
}
