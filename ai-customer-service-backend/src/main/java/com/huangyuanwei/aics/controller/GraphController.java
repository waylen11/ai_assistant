package com.huangyuanwei.aics.controller;

import com.huangyuanwei.aics.common.Result;
import com.huangyuanwei.aics.dto.GraphDefinitionDTO;
import com.huangyuanwei.aics.entity.WorkflowGraph;
import com.huangyuanwei.aics.graph.CustomerServiceGraph;
import com.huangyuanwei.aics.repository.WorkflowGraphRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程编排图接口
 */
@RestController
@RequestMapping("/admin/graph")
@RequiredArgsConstructor
public class GraphController {

    private final WorkflowGraphRepository graphRepository;
    private final CustomerServiceGraph customerServiceGraph;

    @GetMapping
    public Result<List<WorkflowGraph>> list(@RequestParam(required = false) String graphType) {
        if (graphType != null) {
            return Result.ok(graphRepository.findByGraphTypeAndEnabledTrue(graphType));
        }
        return Result.ok(graphRepository.findAll());
    }

    @GetMapping("/default")
    public Result<GraphDefinitionDTO> getDefaultGraph() {
        return Result.ok(customerServiceGraph.getGraphDefinition());
    }

    @GetMapping("/{id}")
    public Result<WorkflowGraph> getById(@PathVariable Long id) {
        return Result.ok(graphRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("流程图不存在")));
    }

    @PostMapping
    public Result<WorkflowGraph> create(@RequestBody WorkflowGraph graph) {
        return Result.ok(graphRepository.save(graph));
    }

    @PutMapping("/{id}")
    public Result<WorkflowGraph> update(@PathVariable Long id, @RequestBody WorkflowGraph req) {
        WorkflowGraph graph = graphRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("流程图不存在"));
        graph.setGraphName(req.getGraphName());
        graph.setDescription(req.getDescription());
        graph.setGraphJson(req.getGraphJson());
        graph.setVersion(graph.getVersion() + 1);
        return Result.ok(graphRepository.save(graph));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        graphRepository.deleteById(id);
        return Result.ok();
    }
}
