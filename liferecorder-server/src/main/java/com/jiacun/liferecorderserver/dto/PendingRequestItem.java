package com.jiacun.liferecorderserver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Pending Request 数据结构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PendingRequestItem {
    private String id;
    private String type;
    private String status;
    private String deviceId;
    private String fileId;
    private String sourceIndex;
    private String reason;
    private String requestedBy;
    private Long createdTime;
    private Long updatedTime;
    private String resultCachedPath;
    private String error;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    
    public String getSourceIndex() { return sourceIndex; }
    public void setSourceIndex(String sourceIndex) { this.sourceIndex = sourceIndex; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    
    public Long getCreatedTime() { return createdTime; }
    public void setCreatedTime(Long createdTime) { this.createdTime = createdTime; }
    
    public Long getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Long updatedTime) { this.updatedTime = updatedTime; }
    
    public String getResultCachedPath() { return resultCachedPath; }
    public void setResultCachedPath(String resultCachedPath) { this.resultCachedPath = resultCachedPath; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
