package com.jiacun.liferecorderserver.dto;

public class AttachmentResponse {
    private boolean success;
    private String attachmentId;
    private String type;
    private String name;
    private String relativePath;
    private String mimeType;
    private Long size;
    private Long createdTime;
    private String message;

    public AttachmentResponse() {
    }

    public AttachmentResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AttachmentResponse(boolean success, String attachmentId, String type, String name,
                               String relativePath, String mimeType, Long size,
                               Long createdTime, String message) {
        this.success = success;
        this.attachmentId = attachmentId;
        this.type = type;
        this.name = name;
        this.relativePath = relativePath;
        this.mimeType = mimeType;
        this.size = size;
        this.createdTime = createdTime;
        this.message = message;
    }

    // 便利构造器（不需要 size/createdTime 的情况）
    public AttachmentResponse(boolean success, String attachmentId, String type, String name,
                               String relativePath, String mimeType, String message) {
        this(success, attachmentId, type, name, relativePath, mimeType, null, null, message);
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getAttachmentId() { return attachmentId; }
    public void setAttachmentId(String attachmentId) { this.attachmentId = attachmentId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public Long getCreatedTime() { return createdTime; }
    public void setCreatedTime(Long createdTime) { this.createdTime = createdTime; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
