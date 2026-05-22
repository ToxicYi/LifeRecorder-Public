package com.jiacun.liferecorderserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * LifeRecorder Workspace Protocol v1 - changes.json 维护服务
 */
@Service
public class LifeChangesService {

    private final WorkspaceService workspaceService;
    private final ObjectMapper objectMapper;
    private static final String CHANGES_FILENAME = "changes.json";

    public LifeChangesService(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 加载今天的 changes.json
     */
    public ChangesData loadTodayChanges() {
        try {
            Path changesPath = workspaceService.getTodayDir().resolve(CHANGES_FILENAME);
            
            if (!Files.exists(changesPath)) {
                // 创建新的 changes
                ChangesData newChanges = createNewChanges();
                saveTodayChanges(newChanges);
                return newChanges;
            }

            String content = Files.readString(changesPath, StandardCharsets.UTF_8);
            return objectMapper.readValue(content, ChangesData.class);

        } catch (IOException e) {
            System.err.println("加载 changes.json 失败: " + e.getMessage());
            return createNewChanges();
        }
    }

    /**
     * 保存今天的 changes.json
     */
    public void saveTodayChanges(ChangesData changes) {
        try {
            Path changesPath = workspaceService.getTodayDir().resolve(CHANGES_FILENAME);
            
            String jsonContent = objectMapper.writeValueAsString(changes);
            Files.writeString(changesPath, jsonContent, StandardCharsets.UTF_8);

        } catch (IOException e) {
            System.err.println("保存 changes.json 失败: " + e.getMessage());
        }
    }

    /**
     * 追加一条 change 记录
     */
    public void appendChange(ChangeEntry change) {
        ChangesData changes = loadTodayChanges();
        
        // 生成唯一 id
        if (change.getId() == null || change.getId().isEmpty()) {
            change.setId("change_" + System.currentTimeMillis());
        }
        
        // 设置创建时间
        if (change.getCreatedTime() == 0) {
            change.setCreatedTime(System.currentTimeMillis());
        }
        
        changes.getChanges().add(change);
        saveTodayChanges(changes);
    }

    /**
     * 创建新的 changes 数据结构
     */
    private ChangesData createNewChanges() {
        ChangesData changes = new ChangesData();
        changes.setSchemaVersion(1);
        changes.setDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        changes.setChanges(new ArrayList<>());
        return changes;
    }

    // ==================== 内部类 ====================

    /**
     * Changes 数据结构
     */
    public static class ChangesData {
        private int schemaVersion;
        private String date;
        private List<ChangeEntry> changes;

        public int getSchemaVersion() { return schemaVersion; }
        public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public List<ChangeEntry> getChanges() { return changes; }
        public void setChanges(List<ChangeEntry> changes) { this.changes = changes; }
    }

    /**
     * Change Entry 数据结构
     */
    public static class ChangeEntry {
        private String id;
        private String type;
        private String targetId;
        private String targetPath;
        private String source;
        private String description;
        private long createdTime;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTargetId() { return targetId; }
        public void setTargetId(String targetId) { this.targetId = targetId; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public long getCreatedTime() { return createdTime; }
        public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }
    }
}
