package com.jiacun.liferecorderserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jiacun.liferecorderserver.dto.WriteAgentResultRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Agent 工具服务 - 受控写入
 * 
 * 通过受控接口接收 OpenClaw Agent 的文件写入请求，写入到 D:\LifeRecorder 工作区。
 * 不允许直接访问文件系统，保证安全性。
 */
@Service
public class AgentToolsService {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private LifeIndexService lifeIndexService;

    @Autowired
    private LifeChangesService lifeChangesService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 安全的文件名正则（只允许字母、数字、下划线、连字符、点和中文）
    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\.\\u4e00-\\u9fa5]+$");
    
    // 禁止的路径分隔符和路径穿越模式
    private static final Pattern PATH_SEPARATOR_PATTERN = Pattern.compile("[/\\\\]");
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("\\.\\.");

    /**
     * 写入 Agent 生成文件
     * 
     * @param request 写入请求
     * @return 写入结果，包含 success、generatedPath、name
     */
    public Map<String, Object> writeAgentResult(WriteAgentResultRequest request) {
        try {
            // 1. 验证并安全化参数
            String type = request.getType().trim().toLowerCase();
            if (!type.equals("markdown") && !type.equals("json")) {
                return Map.of(
                    "success", false,
                    "message", "不支持的文件类型: " + type + "（仅支持 markdown、json）"
                );
            }
            
            String name = request.getName().trim();
            String safeName = sanitizeName(name);
            
            if (safeName.isEmpty()) {
                return Map.of(
                    "success", false,
                    "message", "无效的文件名"
                );
            }
            
            String content = request.getContent();
            String source = request.getSource() != null ? request.getSource().trim() : "openclaw_agent";
            
            // 2. 确定保存目录和路径
            String subdir = type.equals("markdown") ? "markdown" : "json";
            Path targetDir = type.equals("markdown") 
                ? workspaceService.getTodayAiGeneratedMarkdownDir()
                : workspaceService.getTodayAiGeneratedJsonDir();
            
            Path targetFile = targetDir.resolve(safeName);
            String relativePath = "ai_generated/" + subdir + "/" + safeName;
            
            // 3. 安全检查：确保路径在 D:\LifeRecorder 内
            Path rootDir = workspaceService.getRootDir();
            Path normalizedTarget = targetFile.normalize();
            if (!normalizedTarget.startsWith(rootDir)) {
                return Map.of(
                    "success", false,
                    "message", "安全检查失败：禁止写入工作区外路径"
                );
            }
            
            // 4. 创建目录（如不存在）
            Files.createDirectories(targetDir);
            
            // 5. 格式化 JSON 内容（如果是 JSON 类型）
            if (type.equals("json")) {
                try {
                    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
                    Object jsonObj = objectMapper.readValue(content, Object.class);
                    content = objectMapper.writeValueAsString(jsonObj);
                } catch (Exception e) {
                    // JSON 格式化失败，按原样写入
                    System.out.println("[AgentTools] JSON 格式化失败，将按原样写入: " + e.getMessage());
                }
            }
            
            // 6. 写入文件
            Files.writeString(targetFile, content, StandardCharsets.UTF_8);
            System.out.println("[AgentTools] 文件已写入: " + targetFile);
            
            // 7. 计算文件大小
            long fileSize = Files.size(targetFile);
            
            // 8. 更新 index.json 和 changes.json
            updateIndexAndChanges(relativePath, safeName, type, source, content, fileSize);
            
            // 9. 返回成功结果
            return Map.of(
                "success", true,
                "generatedPath", relativePath.replace("\\", "/"),
                "name", safeName
            );
            
        } catch (SecurityException e) {
            System.err.println("[AgentTools] 安全检查失败: " + e.getMessage());
            return Map.of(
                "success", false,
                "message", "安全检查失败: " + e.getMessage()
            );
        } catch (IOException e) {
            System.err.println("[AgentTools] 文件写入失败: " + e.getMessage());
            return Map.of(
                "success", false,
                "message", "文件写入失败: " + e.getMessage()
            );
        } catch (Exception e) {
            System.err.println("[AgentTools] 未知错误: " + e.getMessage());
            return Map.of(
                "success", false,
                "message", "写入失败: " + e.getMessage()
            );
        }
    }

    /**
     * 安全化文件名
     * - 禁止 ../
     * - 禁止 ..\
     * - 禁止绝对路径
     * - 禁止路径分隔符
     * - 只允许普通文件名
     */
    private String sanitizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        
        String sanitized = name.trim();
        
        // 移除路径分隔符
        sanitized = PATH_SEPARATOR_PATTERN.matcher(sanitized).replaceAll("_");
        
        // 移除路径穿越
        sanitized = PATH_TRAVERSAL_PATTERN.matcher(sanitized).replaceAll("_");
        
        // 只保留安全字符
        sanitized = sanitized.replaceAll("[<>:\"|?*]", "_");
        
        // 限制长度（Windows 最大 255 字符，目录前缀占用约 50 字符）
        if (sanitized.length() > 200) {
            int dotIndex = sanitized.lastIndexOf('.');
            if (dotIndex > 0 && sanitized.length() - dotIndex < 10) {
                // 保留扩展名
                String ext = sanitized.substring(dotIndex);
                String nameWithoutExt = sanitized.substring(0, dotIndex);
                sanitized = nameWithoutExt.substring(0, 200 - ext.length()) + ext;
            } else {
                sanitized = sanitized.substring(0, 200);
            }
        }
        
        // 确保不为空
        if (sanitized.isEmpty()) {
            return "";
        }
        
        return sanitized;
    }

    /**
     * 更新 index.json 和 changes.json
     * 字段格式遵循 LifeRecorder Workspace Protocol v1
     */
    private void updateIndexAndChanges(String relativePath, String safeName, String fileType, 
                                       String source, String content, long fileSize) {
        try {
            long now = System.currentTimeMillis();
            String itemId = "gen_" + now;
            String changeId = "change_" + now;
            
            // mimeType 映射
            String mimeType = fileType.equals("markdown") ? "text/markdown" : "application/json";
            
            // preview：内容前 80 字
            String preview = content.length() > 80 ? content.substring(0, 80) : content;
            
            // 1. 更新 index.json（按 relativePath 去重）
            LifeIndexService.IndexData indexData = lifeIndexService.loadTodayIndex();
            
            boolean found = false;
            for (int i = 0; i < indexData.getItems().size(); i++) {
                LifeIndexService.IndexItem item = indexData.getItems().get(i);
                if (item.getRelativePath() != null && 
                    item.getRelativePath().equals(relativePath) && 
                    "ai_generated".equals(item.getType())) {
                    // 更新已有 item
                    item.setName(safeName);
                    item.setMimeType(mimeType);
                    item.setSize(fileSize);
                    item.setSource(source);
                    item.setPreview(preview);
                    item.setUpdatedTime(now);
                    found = true;
                    System.out.println("[AgentTools] 更新已有 index item: " + relativePath);
                    break;
                }
            }
            
            if (!found) {
                // 追加新 item
                LifeIndexService.IndexItem newItem = new LifeIndexService.IndexItem();
                newItem.setId(itemId);
                newItem.setType("ai_generated");
                newItem.setName(safeName);
                newItem.setRelativePath(relativePath);
                newItem.setMimeType(mimeType);
                newItem.setSize(fileSize);
                newItem.setSource(source);
                newItem.setPreview(preview);
                newItem.setCreatedTime(now);
                newItem.setUpdatedTime(now);
                indexData.getItems().add(newItem);
                System.out.println("[AgentTools] 新增 index item: " + relativePath);
            }
            
            lifeIndexService.saveTodayIndex(indexData);
            
            // 2. 更新 changes.json（始终追加新条目）
            LifeChangesService.ChangeEntry newChange = new LifeChangesService.ChangeEntry();
            newChange.setId(changeId);
            newChange.setType("ai_file_generated");
            newChange.setTargetId(found ? findExistingItemId(relativePath) : itemId);
            newChange.setTargetPath(relativePath);
            newChange.setSource(source);
            newChange.setDescription("Agent 生成了文件: " + safeName);
            newChange.setCreatedTime(now);
            
            lifeChangesService.appendChange(newChange);
            System.out.println("[AgentTools] 追加 changes item: " + safeName);
            
        } catch (Exception e) {
            System.err.println("[AgentTools] 更新索引失败: " + e.getMessage());
            // 不算致命错误，继续
        }
    }

    /**
     * 查找已存在 item 的 id
     */
    private String findExistingItemId(String relativePath) {
        try {
            LifeIndexService.IndexData indexData = lifeIndexService.loadTodayIndex();
            for (LifeIndexService.IndexItem item : indexData.getItems()) {
                if (relativePath.equals(item.getRelativePath()) && "ai_generated".equals(item.getType())) {
                    return item.getId();
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        return "gen_" + System.currentTimeMillis();
    }
}