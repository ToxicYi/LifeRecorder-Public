package com.jiacun.liferecorderserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiacun.liferecorderserver.dto.AgentGeneratedFileContentResponse;
import com.jiacun.liferecorderserver.dto.AgentGeneratedFilesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent 生成文件服务
 */
@Service
public class AgentGeneratedFileService {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private LifeIndexService lifeIndexService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取所有 Agent 生成的文件列表（从 index.json 中筛选 type=ai_generated）
     */
    public AgentGeneratedFilesResponse getAgentGeneratedFiles() {
        try {
            // 调试日志：确认读取路径
            Path indexPath = workspaceService.getTodayDir().resolve("index.json");
            System.out.println("[AgentGeneratedFiles] indexPath=" + indexPath);

            // 1. 加载今天的 index.json
            LifeIndexService.IndexData index = lifeIndexService.loadTodayIndex();
            System.out.println("[AgentGeneratedFiles] items count=" + index.getItems().size());

            // 2. 筛选 type=ai_generated 的 items，并补齐缺失字段
            List<AgentGeneratedFilesResponse.AgentGeneratedFileItem> filesFromIndex = new ArrayList<>();
            for (LifeIndexService.IndexItem item : index.getItems()) {
                if ("ai_generated".equals(item.getType())) {
                    AgentGeneratedFilesResponse.AgentGeneratedFileItem fileItem = convertToAgentFileItemWithFallback(item);
                    filesFromIndex.add(fileItem);
                }
            }
            System.out.println("[AgentGeneratedFiles] matched ai_generated count=" + filesFromIndex.size());

            // 3. 扫描实际目录作为兜底
            List<AgentGeneratedFilesResponse.AgentGeneratedFileItem> filesFromScan = scanAiGeneratedDirectories();
            System.out.println("[AgentGeneratedFiles] scanned files count=" + filesFromScan.size());

            // 4. 合并结果，以 relativePath 去重，优先使用 index.json
            java.util.Map<String, AgentGeneratedFilesResponse.AgentGeneratedFileItem> fileMap = new java.util.LinkedHashMap<>();
            
            // 先添加 index.json 中的文件
            for (AgentGeneratedFilesResponse.AgentGeneratedFileItem item : filesFromIndex) {
                fileMap.put(item.getRelativePath(), item);
            }
            
            // 再添加扫描到的文件（如果不存在则添加）
            for (AgentGeneratedFilesResponse.AgentGeneratedFileItem item : filesFromScan) {
                if (!fileMap.containsKey(item.getRelativePath())) {
                    fileMap.put(item.getRelativePath(), item);
                }
            }

            List<AgentGeneratedFilesResponse.AgentGeneratedFileItem> result = new ArrayList<>(fileMap.values());
            System.out.println("[AgentGeneratedFiles] returned files count=" + result.size());

            return new AgentGeneratedFilesResponse(result);

        } catch (Exception e) {
            System.err.println("获取 Agent 生成文件列表失败: " + e.getMessage());
            e.printStackTrace();
            return new AgentGeneratedFilesResponse(new ArrayList<>());
        }
    }

    /**
     * 读取指定 Agent 生成文件的内容
     * @param relativePath 相对路径，例如 ai_generated/markdown/xxx.md
     * @return 文件内容响应
     */
    public AgentGeneratedFileContentResponse getAgentGeneratedFileContent(String relativePath) {
        try {
            System.out.println("[AgentGeneratedFile] requested relativePath=" + relativePath);

            // 安全检查：确保路径在 today/ai_generated 下
            if (relativePath == null || relativePath.trim().isEmpty()) {
                throw new IllegalArgumentException("relativePath 不能为空");
            }

            // 禁止路径穿越
            if (relativePath.contains("..")) {
                throw new SecurityException("禁止路径穿越: " + relativePath);
            }

            // 确保路径以 ai_generated 开头
            if (!relativePath.startsWith("ai_generated/")) {
                throw new SecurityException("只能访问 ai_generated 目录下的文件: " + relativePath);
            }

            // 解析绝对路径
            Path resolvedPath = workspaceService.resolveRelativePath(relativePath);
            System.out.println("[AgentGeneratedFile] resolved path=" + resolvedPath);
            System.out.println("[AgentGeneratedFile] exists=" + Files.exists(resolvedPath));

            // 再次验证路径是否在 today/ai_generated 下
            Path aiGeneratedDir = workspaceService.getTodayDir().resolve("ai_generated").normalize();
            if (!resolvedPath.startsWith(aiGeneratedDir)) {
                throw new SecurityException("禁止访问 ai_generated 之外的路径: " + resolvedPath);
            }

            // 检查文件是否存在
            if (!Files.exists(resolvedPath)) {
                return null; // 返回 null 表示文件不存在
            }

            // 读取文件内容
            String content = Files.readString(resolvedPath, StandardCharsets.UTF_8);

            return new AgentGeneratedFileContentResponse(relativePath, content);

        } catch (SecurityException e) {
            throw e; // 安全异常直接抛出
        } catch (IOException e) {
            System.err.println("读取 Agent 生成文件失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 转换 IndexItem 为 AgentGeneratedFileItem，并补齐缺失字段
     */
    private AgentGeneratedFilesResponse.AgentGeneratedFileItem convertToAgentFileItemWithFallback(LifeIndexService.IndexItem item) {
        AgentGeneratedFilesResponse.AgentGeneratedFileItem fileItem = new AgentGeneratedFilesResponse.AgentGeneratedFileItem();
        
        // id 缺失：gen_ + hash(relativePath)
        String id = item.getId();
        if (id == null || id.trim().isEmpty()) {
            String relativePath = item.getRelativePath() != null ? item.getRelativePath() : "unknown";
            id = "gen_" + Math.abs(relativePath.hashCode());
        }
        fileItem.setId(id);
        
        // name 缺失：从 relativePath 提取文件名
        String name = item.getName();
        if (name == null || name.trim().isEmpty()) {
            String relativePath = item.getRelativePath();
            if (relativePath != null && !relativePath.isEmpty()) {
                int lastSlash = relativePath.lastIndexOf('/');
                name = lastSlash >= 0 ? relativePath.substring(lastSlash + 1) : relativePath;
            } else {
                name = "unknown";
            }
        }
        fileItem.setName(name);
        
        fileItem.setRelativePath(item.getRelativePath());
        
        // mimeType 缺失：根据扩展名推断
        String mimeType = item.getMimeType();
        if (mimeType == null || mimeType.trim().isEmpty()) {
            String relativePath = item.getRelativePath();
            if (relativePath != null) {
                if (relativePath.endsWith(".md")) {
                    mimeType = "text/markdown";
                } else if (relativePath.endsWith(".json")) {
                    mimeType = "application/json";
                } else {
                    mimeType = "application/octet-stream";
                }
            } else {
                mimeType = "application/octet-stream";
            }
        }
        fileItem.setMimeType(mimeType);
        
        // size 缺失：读取实际文件大小
        long size = item.getSize();
        if (size <= 0) {
            try {
                Path filePath = workspaceService.resolveRelativePath(item.getRelativePath());
                if (Files.exists(filePath)) {
                    size = Files.size(filePath);
                }
            } catch (Exception e) {
                // 忽略，保持 size=0
            }
        }
        fileItem.setSize(size);
        
        fileItem.setSource(item.getSource());
        
        // preview 缺失：读取文件前 80 字
        String preview = item.getPreview();
        if (preview == null || preview.trim().isEmpty()) {
            try {
                Path filePath = workspaceService.resolveRelativePath(item.getRelativePath());
                if (Files.exists(filePath)) {
                    String content = Files.readString(filePath, StandardCharsets.UTF_8);
                    preview = content.length() > 80 ? content.substring(0, 80) : content;
                }
            } catch (Exception e) {
                // 忽略，保持 preview=null
            }
        }
        fileItem.setPreview(preview);
        
        // createdTime / updatedTime 缺失：使用文件 lastModified
        long createdTime = item.getCreatedTime();
        long updatedTime = item.getUpdatedTime();
        if (createdTime <= 0 || updatedTime <= 0) {
            try {
                Path filePath = workspaceService.resolveRelativePath(item.getRelativePath());
                if (Files.exists(filePath)) {
                    long lastModified = Files.getLastModifiedTime(filePath).toMillis();
                    if (createdTime <= 0) createdTime = lastModified;
                    if (updatedTime <= 0) updatedTime = lastModified;
                }
            } catch (Exception e) {
                // 忽略，保持时间为 0
            }
        }
        fileItem.setCreatedTime(createdTime);
        fileItem.setUpdatedTime(updatedTime);
        
        return fileItem;
    }

    /**
     * 扫描 ai_generated 目录，返回所有文件
     */
    private List<AgentGeneratedFilesResponse.AgentGeneratedFileItem> scanAiGeneratedDirectories() {
        List<AgentGeneratedFilesResponse.AgentGeneratedFileItem> result = new ArrayList<>();
        
        try {
            // 扫描 markdown 目录
            Path markdownDir = workspaceService.getTodayAiGeneratedMarkdownDir();
            if (Files.exists(markdownDir)) {
                scanDirectory(markdownDir, "ai_generated/markdown", result);
            }
            
            // 扫描 json 目录
            Path jsonDir = workspaceService.getTodayAiGeneratedJsonDir();
            if (Files.exists(jsonDir)) {
                scanDirectory(jsonDir, "ai_generated/json", result);
            }
            
        } catch (Exception e) {
            System.err.println("扫描 ai_generated 目录失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 扫描指定目录下的文件
     */
    private void scanDirectory(Path dir, String basePath, List<AgentGeneratedFilesResponse.AgentGeneratedFileItem> result) {
        try {
            Files.list(dir)
                .filter(Files::isRegularFile)
                .forEach(filePath -> {
                    try {
                        String fileName = filePath.getFileName().toString();
                        String relativePath = basePath + "/" + fileName;
                        
                        AgentGeneratedFilesResponse.AgentGeneratedFileItem item = new AgentGeneratedFilesResponse.AgentGeneratedFileItem();
                        item.setId("gen_" + Math.abs(relativePath.hashCode()));
                        item.setName(fileName);
                        item.setRelativePath(relativePath);
                        
                        // 推断 mimeType
                        if (fileName.endsWith(".md")) {
                            item.setMimeType("text/markdown");
                        } else if (fileName.endsWith(".json")) {
                            item.setMimeType("application/json");
                        } else {
                            item.setMimeType("application/octet-stream");
                        }
                        
                        item.setSize(Files.size(filePath));
                        item.setSource("openclaw_agent");
                        
                        // 读取前 80 字作为 preview
                        String content = Files.readString(filePath, StandardCharsets.UTF_8);
                        item.setPreview(content.length() > 80 ? content.substring(0, 80) : content);
                        
                        long lastModified = Files.getLastModifiedTime(filePath).toMillis();
                        item.setCreatedTime(lastModified);
                        item.setUpdatedTime(lastModified);
                        
                        result.add(item);
                        
                    } catch (Exception e) {
                        System.err.println("扫描文件失败: " + filePath + ", 错误: " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("列出目录失败: " + dir + ", 错误: " + e.getMessage());
        }
    }
}
