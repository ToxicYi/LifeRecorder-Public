package com.jiacun.liferecorderserver.dto;

import java.util.List;

public class ChatPlusRequest {
    private String message;
    private Boolean forceAgent; // 调试字段：强制走 Agent 分支
    private List<AttachmentInfo> attachments; // 用户上传的附件列表

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getForceAgent() {
        return forceAgent;
    }

    public void setForceAgent(Boolean forceAgent) {
        this.forceAgent = forceAgent;
    }

    public List<AttachmentInfo> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentInfo> attachments) {
        this.attachments = attachments;
    }

    /**
     * 附件信息（App 上传后返回的 attachmentId + 相对路径）
     */
    public static class AttachmentInfo {
        private String attachmentId;
        private String name;
        private String relativePath;
        private String mimeType;
        private String type; // file / image

        public AttachmentInfo() {}

        public AttachmentInfo(String attachmentId, String name, String relativePath,
                              String mimeType, String type) {
            this.attachmentId = attachmentId;
            this.name = name;
            this.relativePath = relativePath;
            this.mimeType = mimeType;
            this.type = type;
        }

        public String getAttachmentId() { return attachmentId; }
        public void setAttachmentId(String attachmentId) { this.attachmentId = attachmentId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRelativePath() { return relativePath; }
        public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
