package com.distributed.banking.repository;

import com.distributed.banking.model.LamportEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LamportEventRepository extends JpaRepository<LamportEvent, Long> {
    List<LamportEvent> findTop100ByOrderByLogicalTimestampDesc();
    List<LamportEvent> findTop100ByOrderByCreatedAtDesc();
    List<LamportEvent> findByNodeIdOrderByLogicalTimestampDesc(Long nodeId);
}
