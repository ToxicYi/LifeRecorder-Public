package com.jiacun.liferecorderserver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 手机文件索引上传响应
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhoneFileIndexResponse {
    private boolean success;
    private String message;
    private int totalFiles;
    private int updatedFiles;
    private int newFiles;

    public PhoneFileIndexResponse() {}

    public PhoneFileIndexResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public PhoneFileIndexResponse(boolean success, String message, int totalFiles, int updatedFiles, int newFiles) {
        this.success = success;
        this.message = message;
        this.totalFiles = totalFiles;
        this.updatedFiles = updatedFiles;
        this.newFiles = newFiles;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public int getTotalFiles() { return totalFiles; }
    public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
    
    public int getUpdatedFiles() { return updatedFiles; }
    public void setUpdatedFiles(int updatedFiles) { this.updatedFiles = updatedFiles; }
    
    public int getNewFiles() { return newFiles; }
    public void setNewFiles(int newFiles) { this.newFiles = newFiles; }
}
