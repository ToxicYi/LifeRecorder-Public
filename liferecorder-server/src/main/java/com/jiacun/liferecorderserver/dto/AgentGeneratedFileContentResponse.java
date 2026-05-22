package com.jiacun.liferecorderserver.dto;

/**
 * Agent 生成文件内容响应
 */
public class AgentGeneratedFileContentResponse {
    private String relativePath;
    private String content;

    public AgentGeneratedFileContentResponse() {
    }

    public AgentGeneratedFileContentResponse(String relativePath, String content) {
        this.relativePath = relativePath;
        this.content = content;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
