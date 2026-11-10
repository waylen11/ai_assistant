package com.huangyuanwei.aics.controller;

import com.huangyuanwei.aics.common.Result;
import com.huangyuanwei.aics.entity.AiModel;
import com.huangyuanwei.aics.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型管理接口
 */
@RestController
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    /**
     * 获取已启用模型（公开，前端下拉选择用）
     */
    @GetMapping("/models/enabled")
    public Result<List<AiModel>> listEnabled() {
        return Result.ok(modelService.listEnabled());
    }

    /**
     * 获取全部模型（管理后台）
     */
    @GetMapping("/admin/models")
    public Result<List<AiModel>> listAll() {
        return Result.ok(modelService.listAll());
    }

    @GetMapping("/admin/models/{id}")
    public Result<AiModel> getById(@PathVariable Long id) {
        return Result.ok(modelService.getById(id));
    }

    @PostMapping("/admin/models")
    public Result<AiModel> create(@RequestBody AiModel model) {
        return Result.ok(modelService.create(model));
    }

    @PutMapping("/admin/models/{id}")
    public Result<AiModel> update(@PathVariable Long id, @RequestBody AiModel model) {
        return Result.ok(modelService.update(id, model));
    }

    @DeleteMapping("/admin/models/{id}")
    public Result<?> delete(@PathVariable Long id) {
        modelService.delete(id);
        return Result.ok();
    }

    @PatchMapping("/admin/models/{id}/toggle")
    public Result<?> toggle(@PathVariable Long id) {
        modelService.toggleEnabled(id);
        return Result.ok();
    }
}
