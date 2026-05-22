package com.jiacun.liferecorderserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiacun.liferecorderserver.service.tool.ListTodayFilesTool;
import com.jiacun.liferecorderserver.service.tool.ReadTodaySummaryTool;
import com.jiacun.liferecorderserver.service.tool.SaveMarkdownFileTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Service
public class AgentService {

    @Value("${mimo.api-key:}")
    private String apiKey;

    @Value("${mimo.base-url:https://api.mimo.com/v1}")
    private String baseUrl;

    @Value("${mimo.model:mimo-chat}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final LifeRecorderWorkspace workspace;
    private final ListTodayFilesTool listTodayFilesTool;
    private final ReadTodaySummaryTool readTodaySummaryTool;
    private final SaveMarkdownFileTool saveMarkdownFileTool;

    // 系统提示词：定义 Agent 的能力和工具
    private static final String SYSTEM_PROMPT = """
            你现在是 LifeRecorder Agent。
            LifeRecorder 工作区根目录是：D:\\LifeRecorder。
            所有未以盘符开头的 LifeRecorder 相对路径，都必须按 D:\\LifeRecorder 下的相对路径处理。
            不要把 LifeRecorder 相对路径理解为 OpenClaw 自己的 workspace 路径。
            读取文件时请优先使用绝对路径。
            
            Agent 读取优先级：
            1. D:\\LifeRecorder\\today\\index.json
            2. D:\\LifeRecorder\\today\\changes.json
            3. D:\\LifeRecorder\\phone_sync\\current\\app_files_index.json
            4. D:\\LifeRecorder\\phone_sync\\current\\virtual_folders.json
            5. D:\\LifeRecorder\\phone_sync\\current\\file_index.json
            6. D:\\LifeRecorder\\phone_sync\\current\\photo_index.json
            
            你可以使用以下工具：
            1. list_files - 列出指定日期的所有文件
            2. read_summary - 读取指定日期的 summary.md 文件
            3. save_markdown - 保存 Markdown 文件到 ai_generated 目录
            
            当用户请求时：
            - 如果需要查看今日文件，使用 list_files
            - 如果需要读取今日摘要，使用 read_summary
            - 如果需要生成并保存文件，使用 save_markdown
            
            安全规则：
            - 只能访问 D:/LifeRecorder 目录内的文件
            - 禁止删除任何文件
            - 禁止执行系统命令
            - 禁止覆盖已有文件
            - 不要泄露完整的文件绝对路径
            
            请用中文回复用户。
            """;

    public AgentService(
            LifeRecorderWorkspace workspace,
            ListTodayFilesTool listTodayFilesTool,
            ReadTodaySummaryTool readTodaySummaryTool,
            SaveMarkdownFileTool saveMarkdownFileTool
    ) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.workspace = workspace;
        this.listTodayFilesTool = listTodayFilesTool;
        this.readTodaySummaryTool = readTodaySummaryTool;
        this.saveMarkdownFileTool = saveMarkdownFileTool;
    }

    /**
     * 处理 Agent 请求
     */
    public String processAgentRequest(String userMessage, String dateStr) {
        // 检查配置
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "MiMo 配置缺失，请检查 api-key";
        }

        try {
            // 验证日期
            LocalDate date = workspace.validateDate(dateStr);
            String dateFormatted = date.toString();

            // 构建消息历史
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 系统消息
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);
            messages.add(systemMsg);

            // 检测是否需要调用工具
            String toolResult = detectAndExecuteTool(userMessage, date);
            
            // 如果有工具调用结果，将其添加到上下文中
            if (toolResult != null && !toolResult.isEmpty()) {
                Map<String, String> contextMsg = new HashMap<>();
                contextMsg.put("role", "system");
                contextMsg.put("content", "工具执行结果：" + toolResult);
                messages.add(contextMsg);
            }

            // 用户消息
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            // 调用 MiMo API
            return callMimoApi(messages);

        } catch (Exception e) {
            return "处理请求失败：" + e.getMessage();
        }
    }

    /**
     * 检测并执行工具调用
     */
    private String detectAndExecuteTool(String userMessage, LocalDate date) {
        String lowerMessage = userMessage.toLowerCase();

        // 检测是否需要列出文件
        if (lowerMessage.contains("列出") && (lowerMessage.contains("文件") || lowerMessage.contains("列表"))) {
            return listTodayFilesTool.execute(date);
        }

        // 检测是否需要读取 summary
        if (lowerMessage.contains("读取") && lowerMessage.contains("summary")) {
            return readTodaySummaryTool.execute(date);
        }

        // 检测是否需要保存文件（简单关键词匹配）
        if (lowerMessage.contains("保存") && lowerMessage.contains("文件")) {
            // 提取文件名和内容（简单实现，实际可以用更复杂的解析）
            return extractAndSaveFile(userMessage, date);
        }

        return null; // 不需要调用工具
    }

    /**
     * 提取文件名和内容并保存
     */
    private String extractAndSaveFile(String userMessage, LocalDate date) {
        // 简单实现：从消息中提取文件名和内容
        // 格式示例："保存文件 xxx.md，内容：..."
        
        int saveIndex = userMessage.indexOf("保存");
        int contentIndex = userMessage.indexOf("内容");
        
        if (saveIndex == -1 || contentIndex == -1) {
            return "无法解析保存文件的请求，请使用格式：保存文件 xxx.md，内容：...";
        }

        // 提取文件名
        String filenamePart = userMessage.substring(saveIndex + 2, contentIndex).trim();
        String filename = filenamePart.replace("文件", "").trim();
        
        // 提取内容
        String content = userMessage.substring(contentIndex + 2).trim();
        // 移除可能的前缀符号
        if (content.startsWith("：") || content.startsWith(":")) {
            content = content.substring(1).trim();
        }

        if (filename.isEmpty() || content.isEmpty()) {
            return "文件名或内容为空";
        }

        return saveMarkdownFileTool.execute(date, filename, content);
    }

    /**
     * 调用 MiMo API
     */
    private String callMimoApi(List<Map<String, String>> messages) {
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 确保 URL 正确拼接
            String apiUrl = baseUrl.trim();
            if (apiUrl.endsWith("/")) {
                apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
            }
            String url = apiUrl + "/chat/completions";

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );

            // 解析响应
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            // 检查是否有错误
            if (jsonResponse.has("error")) {
                String errorMessage = jsonResponse.get("error").get("message").asText();
                return "MiMo API 调用失败：" + errorMessage;
            }

            // 提取回复内容
            if (jsonResponse.has("choices") && jsonResponse.get("choices").isArray() && 
                jsonResponse.get("choices").size() > 0) {
                JsonNode firstChoice = jsonResponse.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    return firstChoice.get("message").get("content").asText();
                }
            }

            return "MiMo API 返回格式异常";

        } catch (Exception e) {
            String errorMsg = e.getClass().getSimpleName();
            if (e.getMessage() != null) {
                String safeMessage = e.getMessage().replaceAll(apiKey, "***MASKED***");
                errorMsg += ": " + safeMessage.substring(0, Math.min(safeMessage.length(), 100));
            }
            return "MiMo API 调用失败：" + errorMsg;
        }
    }
}
