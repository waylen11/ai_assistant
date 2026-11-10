package com.huangyuanwei.aics.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.huangyuanwei.aics.common.Result;
import com.huangyuanwei.aics.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 知识库管理接口
 */
@RestController
@RequestMapping("/admin/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    /**
     * 上传文件并返回 Chunk 预览
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws Exception {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(knowledgeService.uploadAndPreview(file, userId));
    }

    /**
     * 获取知识库列表
     */
    @GetMapping
    public Result<?> list() {
        return Result.ok(knowledgeService.listAll());
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.ok();
    }
}
