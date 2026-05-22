package com.jiacun.liferecorderserver.dto;

import java.util.List;

public class ChatPlusResponse {
    private String reply;
    private String mode; // "ai" 或 "agent"
    private String taskType;
    private String generatedPath; // 兼容旧版 App
    private List<GeneratedFile> generatedFiles; // Agent 生成的文件列表

    public ChatPlusResponse() {
    }

    public ChatPlusResponse(String reply, String mode, String taskType, String generatedPath) {
        this.reply = reply;
        this.mode = mode;
        this.taskType = taskType;
        this.generatedPath = generatedPath;
    }

    public ChatPlusResponse(String reply, String mode, String taskType,
                            String generatedPath, List<GeneratedFile> generatedFiles) {
        this.reply = reply;
        this.mode = mode;
        this.taskType = taskType;
        this.generatedPath = generatedPath;
        this.generatedFiles = generatedFiles;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getGeneratedPath() { return generatedPath; }
    public void setGeneratedPath(String generatedPath) { this.generatedPath = generatedPath; }
    public List<GeneratedFile> getGeneratedFiles() { return generatedFiles; }
    public void setGeneratedFiles(List<GeneratedFile> generatedFiles) { this.generatedFiles = generatedFiles; }

    /**
     * Agent 生成的文件信息
     */
    public static class GeneratedFile {
        private String name;
        private String relativePath;
        private String mimeType;
        private Long size;
        private String preview;

        public GeneratedFile() {}

        public GeneratedFile(String name, String relativePath, String mimeType,
                              Long size, String preview) {
            this.name = name;
            this.relativePath = relativePath;
            this.mimeType = mimeType;
            this.size = size;
            this.preview = preview;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRelativePath() { return relativePath; }
        public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }
        public String getPreview() { return preview; }
        public void setPreview(String preview) { this.preview = preview; }
    }
}
