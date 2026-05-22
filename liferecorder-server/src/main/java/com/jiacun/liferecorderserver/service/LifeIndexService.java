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
 * LifeRecorder Workspace Protocol v1 - index.json 维护服务
 */
@Service
public class LifeIndexService {

    private final WorkspaceService workspaceService;
    private final ObjectMapper objectMapper;
    private static final String INDEX_FILENAME = "index.json";

    public LifeIndexService(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 加载今天的 index.json
     */
    public IndexData loadTodayIndex() {
        try {
            Path indexPath = workspaceService.getTodayDir().resolve(INDEX_FILENAME);
            
            if (!Files.exists(indexPath)) {
                // 创建新的 index
                IndexData newIndex = createNewIndex();
                saveTodayIndex(newIndex);
                return newIndex;
            }

            String content = Files.readString(indexPath, StandardCharsets.UTF_8);
            return objectMapper.readValue(content, IndexData.class);

        } catch (IOException e) {
            System.err.println("加载 index.json 失败: " + e.getMessage());
            return createNewIndex();
        }
    }

    /**
     * 保存今天的 index.json
     */
    public void saveTodayIndex(IndexData index) {
        try {
            Path indexPath = workspaceService.getTodayDir().resolve(INDEX_FILENAME);
            index.setUpdatedTime(System.currentTimeMillis());
            
            String jsonContent = objectMapper.writeValueAsString(index);
            Files.writeString(indexPath, jsonContent, StandardCharsets.UTF_8);

        } catch (IOException e) {
            System.err.println("保存 index.json 失败: " + e.getMessage());
        }
    }

    /**
     * 添加或更新 item（相同 id 更新，不同 id 追加）
     */
    public void addOrUpdateItem(IndexItem item) {
        IndexData index = loadTodayIndex();
        
        // 查找是否已存在相同 id
        boolean found = false;
        for (int i = 0; i < index.getItems().size(); i++) {
            if (index.getItems().get(i).getId().equals(item.getId())) {
                index.getItems().set(i, item);
                found = true;
                break;
            }
        }
        
        // 如果不存在，追加
        if (!found) {
            index.getItems().add(item);
        }
        
        saveTodayIndex(index);
    }

    /**
     * 列出所有 items
     */
    public List<IndexItem> listItems() {
        IndexData index = loadTodayIndex();
        return index.getItems();
    }

    /**
     * 创建新的 index 数据结构
     */
    private IndexData createNewIndex() {
        IndexData index = new IndexData();
        index.setSchemaVersion(1);
        index.setDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        index.setUpdatedTime(System.currentTimeMillis());
        index.setItems(new ArrayList<>());
        return index;
    }

    // ==================== 内部类 ====================

    /**
     * Index 数据结构
     */
    public static class IndexData {
        private int schemaVersion;
        private String date;
        private long updatedTime;
        private List<IndexItem> items;

        public int getSchemaVersion() { return schemaVersion; }
        public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public long getUpdatedTime() { return updatedTime; }
        public void setUpdatedTime(long updatedTime) { this.updatedTime = updatedTime; }
        public List<IndexItem> getItems() { return items; }
        public void setItems(List<IndexItem> items) { this.items = items; }
    }

    /**
     * Index Item 数据结构
     */
    public static class IndexItem {
        private String id;
        private String type;
        private String name;
        private String relativePath;
        private String mimeType;
        private long size;
        private String source;
        private String preview;
        private long createdTime;
        private long updatedTime;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRelativePath() { return relativePath; }
        public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getPreview() { return preview; }
        public void setPreview(String preview) { this.preview = preview; }
        public long getCreatedTime() { return createdTime; }
        public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }
        public long getUpdatedTime() { return updatedTime; }
        public void setUpdatedTime(long updatedTime) { this.updatedTime = updatedTime; }
    }
}
