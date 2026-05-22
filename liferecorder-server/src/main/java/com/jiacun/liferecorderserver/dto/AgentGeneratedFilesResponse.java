package com.jiacun.liferecorderserver.dto;

import java.util.List;

/**
 * Agent 生成文件列表响应
 */
public class AgentGeneratedFilesResponse {
    private int schemaVersion;
    private List<AgentGeneratedFileItem> files;

    public AgentGeneratedFilesResponse() {
        this.schemaVersion = 1;
    }

    public AgentGeneratedFilesResponse(List<AgentGeneratedFileItem> files) {
        this.schemaVersion = 1;
        this.files = files;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public List<AgentGeneratedFileItem> getFiles() {
        return files;
    }

    public void setFiles(List<AgentGeneratedFileItem> files) {
        this.files = files;
    }

    /**
     * Agent 生成文件项
     */
    public static class AgentGeneratedFileItem {
        private String id;
        private String name;
        private String relativePath;
        private String mimeType;
        private long size;
        private String source;
        private String preview;
        private long createdTime;
        private long updatedTime;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public void setRelativePath(String relativePath) {
            this.relativePath = relativePath;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getPreview() {
            return preview;
        }

        public void setPreview(String preview) {
            this.preview = preview;
        }

        public long getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(long createdTime) {
            this.createdTime = createdTime;
        }

        public long getUpdatedTime() {
            return updatedTime;
        }

        public void setUpdatedTime(long updatedTime) {
            this.updatedTime = updatedTime;
        }
    }
}
