package com.huangyuanwei.aics.repository;

import com.huangyuanwei.aics.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    List<KnowledgeBase> findByVectorStatus(String vectorStatus);
    List<KnowledgeBase> findAllByOrderByCreatedAtDesc();
}
