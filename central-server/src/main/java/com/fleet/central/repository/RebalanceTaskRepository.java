package com.fleet.central.repository;

import com.fleet.central.domain.RebalanceStatus;
import com.fleet.central.domain.RebalanceTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RebalanceTaskRepository extends JpaRepository<RebalanceTask, String> {
    List<RebalanceTask> findByStatusOrderByCreatedAtDesc(RebalanceStatus status);
}
