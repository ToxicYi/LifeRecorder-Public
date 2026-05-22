package com.jiacun.liferecorderserver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 上传请求文件响应
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadRequestedFileResponse {
    private boolean success;
    private String requestId;
    private String fileId;
    private String cachedPath;
    private String message;

    public UploadRequestedFileResponse() {}

    public UploadRequestedFileResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public UploadRequestedFileResponse(boolean success, String requestId, String fileId, String cachedPath, String message) {
        this.success = success;
        this.requestId = requestId;
        this.fileId = fileId;
        this.cachedPath = cachedPath;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    
    public String getCachedPath() { return cachedPath; }
    public void setCachedPath(String cachedPath) { this.cachedPath = cachedPath; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
