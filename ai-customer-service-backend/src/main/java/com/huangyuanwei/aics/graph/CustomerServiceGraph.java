package com.huangyuanwei.aics.graph;

import com.huangyuanwei.aics.dto.*;
import com.huangyuanwei.aics.enums.GraphConstants;
import com.huangyuanwei.aics.service.KnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SpringAIAlibabaGraph 客服流程编排图定义
 * 节点：意图识别 → 知识检索/业务执行 → 回复生成
 */
@Slf4j
@Component
public class CustomerServiceGraph {
    @Autowired
    private KnowledgeService knowledgeService;

    /**
     * 获取流程图 JSON 定义（供前端可视化渲染）
     */
    public GraphDefinitionDTO getGraphDefinition() {
        GraphDefinitionDTO definition = new GraphDefinitionDTO();
        
        // 初始化节点列表
        List<GraphNodeDTO> nodes = new ArrayList<>();
        
        // 添加意图识别节点
        GraphNodeDTO intentNode = new GraphNodeDTO();
        intentNode.setId(GraphConstants.NODE_INTENT.getValue());
        intentNode.setType(GraphConstants.NODE_TYPE_INTENT.getValue());
        intentNode.setLabel(GraphConstants.NODE_INTENT.getDescription());
        intentNode.setX(100);
        intentNode.setY(200);
        intentNode.setDescription("分析用户输入，识别意图类型（知识查询/业务操作/闲聊）");
        nodes.add(intentNode);
        
        // 添加知识检索节点
        GraphNodeDTO knowledgeNode = new GraphNodeDTO();
        knowledgeNode.setId(GraphConstants.NODE_KNOWLEDGE.getValue());
        knowledgeNode.setType(GraphConstants.NODE_TYPE_KNOWLEDGE.getValue());
        knowledgeNode.setLabel(GraphConstants.NODE_KNOWLEDGE.getDescription());
        knowledgeNode.setX(350);
        knowledgeNode.setY(80);
        knowledgeNode.setDescription("从 Milvus 检索相关知识片段，增强回复质量");
        nodes.add(knowledgeNode);
        
        // 添加业务执行节点
        GraphNodeDTO businessNode = new GraphNodeDTO();
        businessNode.setId(GraphConstants.NODE_BUSINESS.getValue());
        businessNode.setType(GraphConstants.NODE_TYPE_BUSINESS.getValue());
        businessNode.setLabel(GraphConstants.NODE_BUSINESS.getDescription());
        businessNode.setX(350);
        businessNode.setY(320);
        businessNode.setDescription("执行具体业务操作（预约、查询等）");
        nodes.add(businessNode);
        
        // 添加回复生成节点
        GraphNodeDTO replyNode = new GraphNodeDTO();
        replyNode.setId(GraphConstants.NODE_REPLY.getValue());
        replyNode.setType(GraphConstants.NODE_TYPE_REPLY.getValue());
        replyNode.setLabel(GraphConstants.NODE_REPLY.getDescription());
        replyNode.setX(600);
        replyNode.setY(200);
        replyNode.setDescription("综合检索结果生成最终回复");
        nodes.add(replyNode);
        
        definition.setNodes(nodes);
        
        // 初始化边列表
        List<GraphEdgeDTO> edges = new ArrayList<>();
        
        // 添加意图到知识检索的边
        GraphEdgeDTO edge1 = new GraphEdgeDTO();
        edge1.setId("e1");
        edge1.setSource(GraphConstants.NODE_INTENT.getValue());
        edge1.setTarget(GraphConstants.NODE_KNOWLEDGE.getValue());
        edge1.setLabel(GraphConstants.EDGE_KNOWLEDGE.getDescription());
        edge1.setCondition(GraphConstants.EDGE_KNOWLEDGE.getValue());
        edges.add(edge1);
        
        // 添加意图到业务执行的边
        GraphEdgeDTO edge2 = new GraphEdgeDTO();
        edge2.setId("e2");
        edge2.setSource(GraphConstants.NODE_INTENT.getValue());
        edge2.setTarget(GraphConstants.NODE_BUSINESS.getValue());
        edge2.setLabel(GraphConstants.EDGE_BUSINESS.getDescription());
        edge2.setCondition(GraphConstants.EDGE_BUSINESS.getValue());
        edges.add(edge2);
        
        // 添加知识检索到回复生成的边
        GraphEdgeDTO edge3 = new GraphEdgeDTO();
        edge3.setId("e3");
        edge3.setSource(GraphConstants.NODE_KNOWLEDGE.getValue());
        edge3.setTarget(GraphConstants.NODE_REPLY.getValue());
        edge3.setLabel("");
        edges.add(edge3);
        
        // 添加业务执行到回复生成的边
        GraphEdgeDTO edge4 = new GraphEdgeDTO();
        edge4.setId("e4");
        edge4.setSource(GraphConstants.NODE_BUSINESS.getValue());
        edge4.setTarget(GraphConstants.NODE_REPLY.getValue());
        edge4.setLabel("");
        edges.add(edge4);
        
        definition.setEdges(edges);
        
        return definition;
    }

    /**
     * 执行流程图（简化实现，生产环境使用 SpringAIAlibabaGraph StateGraph API）
     *
     * @param userInput 用户输入
     * @param context   上下文（包含 memoryId、modelName 等）
     * @return 流程执行结果
     */
    public GraphExecutionResultDTO execute(String userInput, GraphContext context) {
        log.info("执行客服流程图，输入: {}", userInput);

        // Step 1: 意图识别
        String intent = recognizeIntent(userInput);
        log.info("意图识别结果: {}", intent);

        // Step 2: 根据意图路由
        String retrievedContext = "";
        if (GraphConstants.EDGE_KNOWLEDGE.getValue().equals(intent)) {
            // 走知识检索节点
            retrievedContext = retrieveKnowledge(userInput);
        } else if (GraphConstants.EDGE_BUSINESS.getValue().equals(intent)) {
            // 走业务执行节点
            retrievedContext = executeBusiness(userInput, context);
        }

        // Step 3: 回复生成（返回给上层 ChatService 使用）
        GraphExecutionResultDTO result = new GraphExecutionResultDTO();
        result.setIntent(intent);
        result.setRetrievedContext(retrievedContext);
        result.setNextAction(GraphConstants.NODE_REPLY.getValue());
        
        return result;
    }

    private String recognizeIntent(String input) {
        // 简化版意图识别，生产环境调用 LLM 分类
        String lower = input.toLowerCase();
        if (lower.contains("预约") || lower.contains("联系") || lower.contains("采购") ||
            lower.contains("入库") || lower.contains("出库") || lower.contains("销售")) {
            return GraphConstants.EDGE_BUSINESS.getValue();
        }
        return GraphConstants.EDGE_KNOWLEDGE.getValue();
    }

    private String retrieveKnowledge(String query) {
        // 实际调用 KnowledgeService 的 RAG 检索
        /*log.debug("知识检索: {}", query);
        return "";*/

        log.info("知识检索: {}", query);               // debug → info，让你能在控制台看到
        try {
            // ① 调 KnowledgeService.retrieve，取 top3 最相似的文档块
            List<Document> docs = knowledgeService.retrieve(query, 3);

            // ② 没查到东西就返回空（ChatService 里 buildEnhancedSystemPrompt 会跳过拼接）
            if (docs == null || docs.isEmpty()) {
                log.info("知识检索未找到相关文档");
                return "";
            }

            // ③ 把 List<Document> 里每个文档的 text 取出来，用 \n\n 拼成一整段字符串
            String context = docs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n"));

            log.info("知识检索命中 {} 个文档片段", docs.size());
            return context;                             // 返回给 ChatService，拼进系统提示
        } catch (Exception e) {
            // ④ 检索异常不能让整个对话崩了，打 log 返回空，模型裸答
            log.error("知识检索异常: {}", e.getMessage(), e);
            return "";
        }


    }

    private String executeBusiness(String input, GraphContext context) {
        // 实际调用业务服务
        log.debug("业务执行: {}", input);
        return "";


    }
}
