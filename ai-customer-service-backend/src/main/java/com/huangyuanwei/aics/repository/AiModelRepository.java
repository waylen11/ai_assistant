package com.huangyuanwei.aics.repository;

import com.huangyuanwei.aics.entity.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiModelRepository extends JpaRepository<AiModel, Long> {
    List<AiModel> findByEnabledTrueOrderBySortOrderDesc();
    boolean existsByModelName(String modelName);
    AiModel findByModelNameAndEnabledTrue(String modelName);
    /** 取 sort_order 最高的第一个有效模型，用于启动时加载默认模型 */
    Optional<AiModel> findFirstByEnabledTrueOrderBySortOrderDesc();
}

