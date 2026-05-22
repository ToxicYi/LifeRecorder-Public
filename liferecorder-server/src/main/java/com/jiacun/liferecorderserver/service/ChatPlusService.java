package com.jiacun.liferecorderserver.service;

import com.jiacun.liferecorderserver.dto.AiRouteDecision;
import com.jiacun.liferecorderserver.dto.ChatPlusRequest.AttachmentInfo;
import com.jiacun.liferecorderserver.dto.ChatPlusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * ChatPlus 核心服务 - 统一 AI 入口
 */
@Service
public class ChatPlusService {

    private static final Logger log = Logger.getLogger(ChatPlusService.class.getName());

    @Autowired
    private AiRouterService aiRouterService;

    @Autowired
    private AgentBridgeService agentBridgeService;

    @Autowired
    private AiFinalReplyService aiFinalReplyService;

    @Autowired
    private MimoClient mimoClient; // 复用现有的 MiMo 客户端

    private static final String WORKSPACE_ROOT = "D:/LifeRecorder";

    /**
     * 处理 ChatPlus 请求
     * @param userMessage 用户消息
     * @param forceAgent 是否强制走 Agent
     * @param attachments 用户上传的附件列表（可为 null）
     */
    public ChatPlusResult processChatPlus(String userMessage, Boolean forceAgent,
                                          List<AttachmentInfo> attachments) {
        log.info(String.format("[ChatPlus] message=%s, forceAgent=%s, attachments=%s",
            userMessage, forceAgent, attachments != null ? attachments.size() : 0));

        // 有附件时强制走 Agent
        if (attachments != null && !attachments.isEmpty()) {
            forceAgent = true;
            log.info("[ChatPlus] 有附件，强制走 Agent 模式");
        }
        
        try {
            // 1. 如果 forceAgent == true 或有附件，跳过路由判断，直接走 Agent
            if (Boolean.TRUE.equals(forceAgent)) {
                log.info("[ChatPlus] forceAgent=true, skip router");

                // 构造强制 Agent 决策
                AiRouteDecision decision = new AiRouteDecision();
                decision.setRoute("agent");
                decision.setTool("call_agent");
                decision.setTaskType("debug_agent");
                decision.setUserIntent(userMessage);
                decision.setNeedAttachments(attachments != null && !attachments.isEmpty());
                decision.setExpectedOutput("text_reply");
                decision.setUrgency("sync");
                decision.setReason(attachments != null && !attachments.isEmpty()
                    ? "用户上传了附件，需要 Agent 分析"
                    : "用户强制测试 Agent");

                return handleAgentMode(userMessage, decision, true, attachments); // 有附件时不整理回复
            }

            // 2. 正常路由决策
            AiRouteDecision decision;
            try {
                decision = aiRouterService.decide(userMessage);
            } catch (Exception e) {
                // Router 调用失败，返回错误响应
                log.severe("AI Router 调用失败: " + e.getMessage());
                e.printStackTrace(); // 调试信息只打印到控制台
                return new ChatPlusResult(
                    "AI Router 调用失败，暂时无法判断是否需要调用 Agent，请稍后重试。",
                    "error",
                    "route_error",
                    null
                );
            }

            if (decision == null) {
                // Router 返回 null，也视为失败
                log.severe("AI Router 返回 null");
                return new ChatPlusResult(
                    "AI Router 调用失败，暂时无法判断是否需要调用 Agent，请稍后重试。",
                    "error",
                    "route_error",
                    null
                );
            }

            // 3. 根据路由决策执行
            if ("agent".equals(decision.getRoute())) {
                return handleAgentMode(userMessage, decision, false, attachments); // 正常 Agent 模式需要整理回复
            } else {
                return handleAiMode(userMessage, decision.getTaskType());
            }

        } catch (Exception e) {
            System.err.println("ChatPlus 处理失败: " + e.getMessage());
            return new ChatPlusResult(
                "处理失败：" + e.getMessage(),
                "ai",
                "chat",
                null
            );
        }
    }

    /**
     * 处理普通 AI 模式
     */
    private ChatPlusResult handleAiMode(String userMessage, String taskType) {
        String reply = mimoClient.chatWithMimo(userMessage);
        return new ChatPlusResult(reply, "ai", taskType, null);
    }

    /**
     * 处理 Agent 模式
     * @param decision 路由决策
     * @param skipReplyFormatting 是否跳过回复整理（forceAgent=true 或有附件时为 true）
     * @param attachments 用户上传的附件列表
     */
    private ChatPlusResult handleAgentMode(String userMessage, AiRouteDecision decision,
                                           boolean skipReplyFormatting,
                                           List<AttachmentInfo> attachments) {
        try {
            // 1. 记录 Agent 开始时间
            long agentStartTimeMillis = System.currentTimeMillis();
            log.info("[GeneratedPath] startTime=" + agentStartTimeMillis);

            // 2. 调用 Agent 前，记录当前已有的 Agent 生成文件
            Map<String, FileInfo> filesBefore = scanAiGeneratedFilesWithInfo();
            log.info("[GeneratedPath] before count=" + filesBefore.size());

            // 3. 调用 Agent（注入附件信息）
            String agentResult;
            if (attachments != null && !attachments.isEmpty()) {
                // 有附件时，注入附件上下文到 userIntent
                String attachmentContext = buildAttachmentContext(attachments);
                decision.setUserIntent(userMessage + "\n\n" + attachmentContext);
                agentResult = agentBridgeService.callAgent(decision);
            } else {
                agentResult = agentBridgeService.callAgent(decision);
            }

            // 4. 解析 Agent 回复，提取用户可读文本
            String parsedReply = parseAgentReply(agentResult);
            if (parsedReply != null) {
                agentResult = parsedReply;
            }

            // 检查是否超时
            if (agentResult.contains("超时")) {
                return new ChatPlusResult(
                    "这个问题我将调用 Agent 来回答。\n\nAgent 执行超时，任务未完成。",
                    "agent",
                    decision.getTaskType(),
                    null
                );
            }

            // 4. Agent 执行完成后，再扫描一次目录
            Map<String, FileInfo> filesAfter = scanAiGeneratedFilesWithInfo();
            log.info("[GeneratedPath] after count=" + filesAfter.size());

            // 5. 找出新生成或更新的文件
            String generatedPath = findNewOrUpdatedFile(filesBefore, filesAfter, agentStartTimeMillis);
            if (generatedPath == null) {
                log.info("[GeneratedPath] no LifeRecorder generated file detected");
            }
            log.info("[GeneratedPath] selected=" + (generatedPath != null ? generatedPath : "null"));

            String finalReply;
            if (skipReplyFormatting) {
                // forceAgent=true 时，直接返回 Agent 原始结果
                finalReply = agentResult;
                log.info("[ChatPlus] forceAgent=true, returning raw agent result");
            } else {
                // 正常 Agent 模式，整理回复并添加提示
                String formattedReply = aiFinalReplyService.buildReply(agentResult);
                finalReply = "这个问题我将调用 Agent 来回答。\n\n" + formattedReply;
            }

            // 6. 构建 generatedFiles 列表（如果有）
            List<ChatPlusResponse.GeneratedFile> generatedFiles = null;
            if (generatedPath != null) {
                FileInfo info = filesAfter.get(generatedPath);
                String mimeType = inferMimeType(generatedPath);
                String preview = buildFilePreview(generatedPath, info);
                generatedFiles = new ArrayList<>();
                generatedFiles.add(new ChatPlusResponse.GeneratedFile(
                    getFileNameFromPath(generatedPath),
                    generatedPath,
                    mimeType,
                    info != null ? info.getSize() : null,
                    preview
                ));
            }

            return new ChatPlusResult(finalReply, "agent", decision.getTaskType(),
                                      generatedPath, generatedFiles);

        } catch (Exception e) {
            log.severe("Agent 模式处理失败: " + e.getMessage());
            return new ChatPlusResult(
                "这个问题我将调用 Agent 来回答。\n\nAgent 调用失败：" + e.getMessage(),
                "agent",
                decision.getTaskType(),
                null
            );
        }
    }

    /**
     * 文件信息类
     */
    private static class FileInfo {
        private final String relativePath;
        private final long lastModified;
        private final long size;

        public FileInfo(String relativePath, long lastModified, long size) {
            this.relativePath = relativePath;
            this.lastModified = lastModified;
            this.size = size;
        }

        public String getRelativePath() { return relativePath; }
        public long getLastModified() { return lastModified; }
        public long getSize() { return size; }
    }

    /**
     * 扫描 ai_generated 目录，返回文件路径 -> FileInfo 的映射
     */
    private Map<String, FileInfo> scanAiGeneratedFilesWithInfo() {
        Map<String, FileInfo> fileMap = new HashMap<>();
        
        try {
            // 扫描 markdown 目录
            Path markdownDir = Paths.get(WORKSPACE_ROOT, "today/ai_generated/markdown");
            if (Files.exists(markdownDir)) {
                scanDirectoryWithInfo(markdownDir, "ai_generated/markdown", fileMap);
            }
            
            // 扫描 json 目录
            Path jsonDir = Paths.get(WORKSPACE_ROOT, "today/ai_generated/json");
            if (Files.exists(jsonDir)) {
                scanDirectoryWithInfo(jsonDir, "ai_generated/json", fileMap);
            }
            
        } catch (Exception e) {
            log.warning("扫描 ai_generated 目录失败: " + e.getMessage());
        }
        
        return fileMap;
    }

    /**
     * 扫描指定目录下的文件（包含 size 信息）
     */
    private void scanDirectoryWithInfo(Path dir, String basePath, Map<String, FileInfo> fileMap) {
        try {
            Files.list(dir)
                .filter(Files::isRegularFile)
                .forEach(filePath -> {
                    try {
                        String fileName = filePath.getFileName().toString();
                        String relativePath = basePath + "/" + fileName;
                        long lastModified = Files.getLastModifiedTime(filePath).toMillis();
                        long size = Files.size(filePath);
                        fileMap.put(relativePath, new FileInfo(relativePath, lastModified, size));
                    } catch (IOException e) {
                        log.warning("获取文件信息失败: " + filePath + ", 错误: " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            log.warning("列出目录失败: " + dir + ", 错误: " + e.getMessage());
        }
    }

    /**
     * 找出新生成或更新的文件
     * @param filesBefore Agent 调用前的文件快照
     * @param filesAfter Agent 调用后的文件快照
     * @param agentStartTimeMillis Agent 开始时间
     * @return 最新文件的相对路径，如果没有新文件则返回 null
     */
    private String findNewOrUpdatedFile(Map<String, FileInfo> filesBefore, Map<String, FileInfo> filesAfter, long agentStartTimeMillis) {
        List<FileInfo> newCandidates = new ArrayList<>();
        List<FileInfo> updatedCandidates = new ArrayList<>();
        List<FileInfo> timeWindowCandidates = new ArrayList<>();
        
        // 优先级 A：新增文件
        for (Map.Entry<String, FileInfo> entry : filesAfter.entrySet()) {
            String relativePath = entry.getKey();
            FileInfo afterInfo = entry.getValue();
            
            if (!filesBefore.containsKey(relativePath)) {
                newCandidates.add(afterInfo);
            }
        }
        
        // 优先级 B：更新文件
        for (Map.Entry<String, FileInfo> entry : filesAfter.entrySet()) {
            String relativePath = entry.getKey();
            FileInfo afterInfo = entry.getValue();
            FileInfo beforeInfo = filesBefore.get(relativePath);
            
            if (beforeInfo != null) {
                // lastModified 变化 或 size 变化
                if (afterInfo.getLastModified() > beforeInfo.getLastModified() || 
                    afterInfo.getSize() != beforeInfo.getSize()) {
                    updatedCandidates.add(afterInfo);
                }
            }
        }
        
        // 优先级 C：时间窗口兜底（Agent 开始后修改的文件）
        long timeWindowThreshold = agentStartTimeMillis - 5000; // 提前 5 秒容差
        for (Map.Entry<String, FileInfo> entry : filesAfter.entrySet()) {
            FileInfo afterInfo = entry.getValue();
            if (afterInfo.getLastModified() >= timeWindowThreshold) {
                timeWindowCandidates.add(afterInfo);
            }
        }
        
        log.info("[GeneratedPath] new candidates=" + newCandidates.size());
        log.info("[GeneratedPath] updated candidates=" + updatedCandidates.size());
        
        // 选择策略：优先新增，其次更新，最后时间窗口
        FileInfo selected = null;
        
        if (!newCandidates.isEmpty()) {
            selected = findLatestFile(newCandidates);
        } else if (!updatedCandidates.isEmpty()) {
            selected = findLatestFile(updatedCandidates);
        } else if (!timeWindowCandidates.isEmpty()) {
            selected = findLatestFile(timeWindowCandidates);
        }
        
        return selected != null ? selected.getRelativePath() : null;
    }

    /**
     * 从候选列表中选择 lastModified 最新的文件
     */
    private FileInfo findLatestFile(List<FileInfo> candidates) {
        FileInfo latest = null;
        long latestTime = 0;
        
        for (FileInfo info : candidates) {
            if (info.getLastModified() > latestTime) {
                latestTime = info.getLastModified();
                latest = info;
            }
        }
        
        return latest;
    }

    /**
     * 根据文件扩展名推断 MIME 类型
     */
    private String inferMimeType(String relativePath) {
        if (relativePath == null) return "application/octet-stream";
        String lower = relativePath.toLowerCase();
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) return "text/markdown";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".xml")) return "application/xml";
        if (lower.endsWith(".csv")) return "text/csv";
        return "application/octet-stream";
    }

    /**
     * 从相对路径中提取文件名（Java 兼容版本）
     */
    private String getFileNameFromPath(String relativePath) {
        if (relativePath == null) return "unknown";
        int lastSlash = relativePath.lastIndexOf('/');
        return lastSlash >= 0 ? relativePath.substring(lastSlash + 1) : relativePath;
    }

    /**
     * 构建文件预览文本（取文件内容前 200 字符）
     */
    private String buildFilePreview(String relativePath, FileInfo info) {
        try {
            Path filePath = Paths.get(WORKSPACE_ROOT, relativePath.replace("/", "\\"));
            if (Files.exists(filePath) && Files.size(filePath) < 50_000) {
                String content = new String(Files.readAllBytes(filePath), "UTF-8");
                String preview = content.trim().replace("\\r\\n", " ").replace("\\n", " ");
                if (preview.length() > 200) {
                    preview = preview.substring(0, 200) + "…";
                }
                return preview;
            }
        } catch (Exception e) {
            log.warning("读取文件预览失败: " + relativePath);
        }
        return "文件已生成";
    }

    /**
     * 解析 Agent 回复，提取用户可读文本
     * 使用手动解析，不依赖外部 JSON 库
     */
    private String parseAgentReply(String rawReply) {
        if (rawReply == null || rawReply.trim().isEmpty()) {
            return null;
        }

        String trimmed = rawReply.trim();
        log.info("[AgentReply] raw length=" + trimmed.length());

        // 1. 如果不是 JSON 格式，直接返回
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            log.info("[AgentReply] not JSON, returning raw");
            return trimmed;
        }

        try {
            // 2. 是 JSON，尝试提取文本字段
            String[] textFields = {"\"text\"", "\"content\"", "\"message\"", "\"reply\"", "\"response\"", "\"output\"", "\"result\""};
            for (String field : textFields) {
                String value = extractJsonStringValue(trimmed, field);
                if (value != null && !value.isEmpty()) {
                    log.info("[AgentReply] parsed from " + field + "=" + value.substring(0, Math.min(50, value.length())) + "...");
                    return value;
                }
            }

            // 3. 尝试提取 payloads[].text
            if (trimmed.contains("\"payloads\"")) {
                String value = extractJsonArrayStringValue(trimmed, "payloads", "text");
                if (value != null) {
                    log.info("[AgentReply] parsed from payloads[].text");
                    return value;
                }
            }

            // 4. 尝试提取 completion.text
            if (trimmed.contains("\"completion\"")) {
                String value = extractJsonNestedValue(trimmed, "completion", "text");
                if (value != null) {
                    log.info("[AgentReply] parsed from completion.text");
                    return value;
                }
            }

            // 解析失败
            log.warning("[AgentReply] parse failed, no text field found");
            return null;

        } catch (Exception e) {
            log.warning("[AgentReply] parse failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从 JSON 中提取字符串值（简化版）
     */
    private String extractJsonStringValue(String json, String fieldName) {
        String pattern = fieldName + "\":\"";
        int start = json.indexOf(pattern);
        if (start < 0) {
            // 尝试 "field": "
            pattern = fieldName + "\": \"";
            start = json.indexOf(pattern);
        }
        if (start < 0) return null;

        int valueStart = start + pattern.length();
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueEnd < 0) return null;

        return json.substring(valueStart, valueEnd);
    }

    /**
     * 从 JSON 数组中提取字符串值
     */
    private String extractJsonArrayStringValue(String json, String arrayName, String fieldName) {
        int arrayStart = json.indexOf("\"" + arrayName + "\":[");
        if (arrayStart < 0) return null;

        int objStart = json.indexOf("{", arrayStart);
        if (objStart < 0 || objStart > arrayStart + 100) return null;

        return extractJsonStringValue(json.substring(objStart), "\"" + fieldName + "\"");
    }

    /**
     * 从嵌套 JSON 对象中提取字符串值
     */
    private String extractJsonNestedValue(String json, String outerField, String innerField) {
        int outerStart = json.indexOf("\"" + outerField + "\":{");
        if (outerStart < 0) return null;

        int objStart = json.indexOf("{", outerStart);
        if (objStart < 0) return null;

        int objEnd = findMatchingBrace(json, objStart);
        if (objEnd < 0) return null;

        String innerObj = json.substring(objStart, objEnd + 1);
        return extractJsonStringValue(innerObj, "\"" + innerField + "\"");
    }

    /**
     * 找到匹配的闭合花括号
     */
    private int findMatchingBrace(String json, int openIndex) {
        int depth = 0;
        boolean inString = false;
        for (int i = openIndex; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    /**
     * 构建附件上下文信息，注入给 Agent
     */
    private String buildAttachmentContext(List<AttachmentInfo> attachments) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n=== 用户上传的附件 ===");
        for (int i = 0; i < attachments.size(); i++) {
            AttachmentInfo att = attachments.get(i);
            sb.append(String.format(
                "\n附件 %d：\n  名称：%s\n  相对路径：D:\\LifeRecorder\\today\\%s\n  类型：%s\n  MIME：%s",
                i + 1,
                att.getName(),
                att.getRelativePath(),
                att.getType(),
                att.getMimeType()
            ));
        }
        sb.append("\n\n请先读取 today/index.json 和 today/changes.json，再按需读取以上附件的 relativePath。");
        return sb.toString();
    }

    /**
     * ChatPlus 结果封装类
     */
    public static class ChatPlusResult {
        private String reply;
        private String mode;
        private String taskType;
        private String generatedPath;
        private List<ChatPlusResponse.GeneratedFile> generatedFiles;

        public ChatPlusResult(String reply, String mode, String taskType, String generatedPath) {
            this.reply = reply;
            this.mode = mode;
            this.taskType = taskType;
            this.generatedPath = generatedPath;
        }

        public ChatPlusResult(String reply, String mode, String taskType, String generatedPath,
                              List<ChatPlusResponse.GeneratedFile> generatedFiles) {
            this.reply = reply;
            this.mode = mode;
            this.taskType = taskType;
            this.generatedPath = generatedPath;
            this.generatedFiles = generatedFiles;
        }

        public String getReply() { return reply; }
        public String getMode() { return mode; }
        public String getTaskType() { return taskType; }
        public String getGeneratedPath() { return generatedPath; }
        public List<ChatPlusResponse.GeneratedFile> getGeneratedFiles() { return generatedFiles; }
    }
}
