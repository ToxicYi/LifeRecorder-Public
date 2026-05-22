package com.jiacun.liferecorderserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiacun.liferecorderserver.dto.AiRouteDecision;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI 路由服务 - 根据用户消息内容判断应该走普通 AI 还是 Agent
 */
@Service
public class AiRouterService {

    @Value("${mimo.api-key:}")
    private String apiKey;

    @Value("${mimo.base-url:https://token-plan-cn.xiaomimimo.com/v1}")
    private String baseUrl;

    @Value("${mimo.model:mimo-v2.5}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // 工作区读取关键词
    private static final String[] WORKSPACE_KEYWORDS = {
        "读取今天记录", "今日总结", "每日总结", "读取 LifeRecorder",
        "读取工作区", "读取 today", "读取 index.json", "读取 changes.json"
    };

    // 文件相关关键词 - 增强版，覆盖更多 Agent 场景
    private static final String[] FILE_KEYWORDS = {
        // 读取/总结类
        "读取文件", "总结文件", "读取 app_files_index", "app_files_index",
        "文件列表", "phone_sync", "cachedPath", "fileId",
        // 手机文件类
        "上传的文件", "已缓存文件", "Agent 文件请求",
        "App 文件页", "手机文件", "虚拟文件夹",
        // 扩展关键词（任务描述）
        "总结", "读取", "生成文件", "生成一份", "保存",
        // 文件扩展名
        ".txt", ".md", ".pdf", ".doc", ".docx",
        // 特定文件操作
        "android_test_file", "android_real_test", "pending_request",
        "phone_sync/current", "virtual_folders", "photo_index",
        // Agent 相关
        "App 文件页里有哪些", "已生成文件", "生成 Markdown 总结"
    };

    // 文件类关键词 - 识别用户想分析/处理文件
    private static final String[] FILE_PROCESSING_KEYWORDS = {
        // 文件操作
        "分析这个文件", "总结这个文件", "读取这个文件", "生成文件", "保存成文件",
        // 文件类型
        "附件", "上传文件", "上传图片", "上传照片", "带附件",
        // 总结/生成
        "生成总结", "生成报告", "生成摘要", "总结成", "分析图片", "分析附件",
        "看看这个文件", "处理这个文件"
    };

    // 生成文件关键词
    private static final String[] GENERATE_KEYWORDS = {
        "生成 Markdown", "生成总结文件", "保存为文件", "写入 ai_generated",
        "更新 agent_status", "生成 Markdown 总结", "生成一份 Markdown",
        "生成 JSON", "生成报告", "生成摘要"
    };

    private static final String ROUTER_PROMPT = """
            你是 LifeRecorder 的 AI 路由器。
            
            你只能判断用户请求应该由普通 AI 直接回答，还是需要调用 Agent 工具。
            
            AI 适合：
            - 普通聊天
            - 概念解释
            - 写作建议
            - 不需要读取用户文件的问题
            - 不需要生成文件的问题
            
            Agent 适合：
            - 需要读取 D:/LifeRecorder/today/index.json
            - 需要读取 changes.json、daily_context.json、chat_history.json
            - 需要读取用户上传的图片或文件
            - 需要生成 Markdown / JSON 文件
            - 需要总结今天
            - 需要查找照片或文件
            - 需要操作 LifeRecorder 工作区文件
            
            你必须只返回 JSON，不要返回 Markdown，不要解释。
            
            返回格式必须是：
            {
              "route": "ai 或 agent",
              "tool": "call_agent 或 null",
              "taskType": "chat 或 daily_summary 或 image_to_markdown 或 file_to_markdown 或 photo_search 或 file_search 或 read_today_context 或 generate_file",
              "userIntent": "一句话概括用户意图",
              "needAttachments": true,
              "expectedOutput": "text_reply 或 markdown_file 或 json_file 或 search_result",
              "urgency": "sync 或 async",
              "reason": "简短原因"
            }
            
            用户请求：{userMessage}
            """;

    public AiRouterService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 判断用户请求的路由
     */
    public AiRouteDecision decide(String userMessage) {
        try {
            // 基于关键词的快速路由判断
            return decideByKeywords(userMessage);

        } catch (Exception e) {
            System.err.println("AI Router 失败，默认走普通 AI: " + e.getMessage());
            return createDefaultAiDecision(userMessage);
        }
    }

    /**
     * 基于关键词的路由判断
     */
    private AiRouteDecision decideByKeywords(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();
        
        // 检查工作区读取关键词
        for (String keyword : WORKSPACE_KEYWORDS) {
            if (lowerMessage.contains(keyword.toLowerCase())) {
                AiRouteDecision decision = createAgentDecision(userMessage, "workspace_read", 
                    "包含工作区读取关键词: " + keyword);
                logRouteDecision(decision);
                return decision;
            }
        }
        
        // 检查文件相关关键词
        for (String keyword : FILE_KEYWORDS) {
            if (lowerMessage.contains(keyword.toLowerCase())) {
                AiRouteDecision decision = createAgentDecision(userMessage, "file_operation", 
                    "包含文件操作关键词: " + keyword);
                logRouteDecision(decision);
                return decision;
            }
        }
        
        // 检查文件处理关键词（上传附件后分析等场景）
        for (String keyword : FILE_PROCESSING_KEYWORDS) {
            if (lowerMessage.contains(keyword.toLowerCase())) {
                AiRouteDecision decision = createAgentDecision(userMessage, "file_processing",
                    "包含文件处理关键词: " + keyword);
                logRouteDecision(decision);
                return decision;
            }
        }

        // 检查生成文件关键词
        for (String keyword : GENERATE_KEYWORDS) {
            if (lowerMessage.contains(keyword.toLowerCase())) {
                AiRouteDecision decision = createAgentDecision(userMessage, "generate_file", 
                    "包含生成文件关键词: " + keyword);
                logRouteDecision(decision);
                return decision;
            }
        }
        
        // 默认走普通 AI
        AiRouteDecision decision = createDefaultAiDecision(userMessage);
        logRouteDecision(decision);
        return decision;
    }

    /**
     * 创建 Agent 决策
     */
    private AiRouteDecision createAgentDecision(String userMessage, String taskType, String reason) {
        AiRouteDecision decision = new AiRouteDecision();
        decision.setRoute("agent");
        decision.setTool("call_agent");
        decision.setTaskType(taskType);
        decision.setUserIntent(userMessage);
        decision.setNeedAttachments(false);
        decision.setExpectedOutput("text_reply");
        decision.setUrgency("sync");
        // 统一 reason 格式：如果是文件/总结类任务，使用 file_or_summary_task
        if (taskType != null && (taskType.contains("file") || taskType.contains("workspace") || taskType.contains("generate"))) {
            decision.setReason("file_or_summary_task");
        } else {
            decision.setReason(reason);
        }
        return decision;
    }

    /**
     * 打印路由决策日志
     */
    private void logRouteDecision(AiRouteDecision decision) {
        System.out.println(String.format("[AiRouter] mode=%s taskType=%s reason=%s",
            decision.getRoute(), decision.getTaskType(), decision.getReason()));
    }

    /**
     * 解析路由决策 JSON
     */
    private AiRouteDecision parseRouteDecision(String jsonContent) {
        try {
            // 清理可能的 Markdown 代码块标记
            String cleaned = jsonContent.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            }
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            JsonNode jsonNode = objectMapper.readTree(cleaned);
            
            AiRouteDecision decision = new AiRouteDecision();
            decision.setRoute(jsonNode.has("route") ? jsonNode.get("route").asText() : "ai");
            decision.setTool(jsonNode.has("tool") && !jsonNode.get("tool").isNull() ? jsonNode.get("tool").asText() : null);
            decision.setTaskType(jsonNode.has("taskType") ? jsonNode.get("taskType").asText() : "chat");
            decision.setUserIntent(jsonNode.has("userIntent") ? jsonNode.get("userIntent").asText() : "");
            decision.setNeedAttachments(jsonNode.has("needAttachments") && jsonNode.get("needAttachments").asBoolean());
            decision.setExpectedOutput(jsonNode.has("expectedOutput") ? jsonNode.get("expectedOutput").asText() : "text_reply");
            decision.setUrgency(jsonNode.has("urgency") ? jsonNode.get("urgency").asText() : "sync");
            decision.setReason(jsonNode.has("reason") ? jsonNode.get("reason").asText() : "");

            return decision;

        } catch (Exception e) {
            System.err.println("解析路由决策 JSON 失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 创建默认的 AI 决策
     */
    private AiRouteDecision createDefaultAiDecision(String userMessage) {
        AiRouteDecision decision = new AiRouteDecision();
        decision.setRoute("ai");
        decision.setTool(null);
        decision.setTaskType("chat");
        decision.setUserIntent(userMessage);
        decision.setNeedAttachments(false);
        decision.setExpectedOutput("text_reply");
        decision.setUrgency("sync");
        decision.setReason("路由判断失败，默认走普通 AI");
        return decision;
    }
}
