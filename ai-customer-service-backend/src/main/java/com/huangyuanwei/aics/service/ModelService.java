package com.huangyuanwei.aics.service;

import com.huangyuanwei.aics.config.ChatClientFactory;
import com.huangyuanwei.aics.entity.AiModel;
import com.huangyuanwei.aics.repository.AiModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 模型管理服务
 */
@Service
@RequiredArgsConstructor
public class ModelService {

    private final AiModelRepository aiModelRepository;
    private final ChatClientFactory chatClientFactory;

    /**
     * 获取所有启用的模型（供前端选择）
     */
    public List<AiModel> listEnabled() {
        return aiModelRepository.findByEnabledTrueOrderBySortOrderDesc();
    }

    /**
     * 获取全部模型列表（管理后台）
     */
    public List<AiModel> listAll() {
        return aiModelRepository.findAll();
    }

    public AiModel getById(Long id) {
        return aiModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模型不存在"));
    }

    @Transactional
    public AiModel create(AiModel model) {
        if (aiModelRepository.existsByModelName(model.getModelName())) {
            throw new IllegalArgumentException("模型名称已存在");
        }
        return aiModelRepository.save(model);
    }

    @Transactional
    public AiModel update(Long id, AiModel req) {
        AiModel model = getById(id);
        String oldModelName = model.getModelName();
        if (req.getModelType() != null) model.setModelType(req.getModelType());
        if (req.getModelName() != null) model.setModelName(req.getModelName());
        if (req.getProvider() != null) model.setProvider(req.getProvider());
        if (req.getDescription() != null) model.setDescription(req.getDescription());
        if (req.getApiKey() != null) model.setApiKey(req.getApiKey());
        if (req.getBaseUrl() != null) model.setBaseUrl(req.getBaseUrl());
        if (req.getMultimodal() != null) model.setMultimodal(req.getMultimodal());
        if (req.getIconUrl() != null) model.setIconUrl(req.getIconUrl());
        if (req.getEnabled() != null) model.setEnabled(req.getEnabled());
        if (req.getSortOrder() != null) model.setSortOrder(req.getSortOrder());
        AiModel saved = aiModelRepository.save(model);
        // 清除旧缓存（apiKey / baseUrl 可能已变更）
        chatClientFactory.evictCache(oldModelName);
        if (!oldModelName.equals(saved.getModelName())) {
            chatClientFactory.evictCache(saved.getModelName());
        }
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        AiModel model = getById(id);
        aiModelRepository.deleteById(id);
        chatClientFactory.evictCache(model.getModelName());
    }

    @Transactional
    public void toggleEnabled(Long id) {
        AiModel model = getById(id);
        model.setEnabled(!model.getEnabled());
        aiModelRepository.save(model);
        chatClientFactory.evictCache(model.getModelName());
    }
}
