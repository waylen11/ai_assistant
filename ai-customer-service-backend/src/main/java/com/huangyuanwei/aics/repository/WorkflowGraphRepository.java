package com.huangyuanwei.aics.repository;

import com.huangyuanwei.aics.entity.WorkflowGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowGraphRepository extends JpaRepository<WorkflowGraph, Long> {
    List<WorkflowGraph> findByGraphTypeAndEnabledTrue(String graphType);
    Optional<WorkflowGraph> findFirstByGraphTypeAndEnabledTrueOrderByVersionDesc(String graphType);
}
