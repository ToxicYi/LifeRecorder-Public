package com.jiacun.liferecorderserver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 创建文件获取请求
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateFetchRequest {
    private String deviceId;
    private String fileId;
    private String sourceIndex;
    private String reason;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    
    public String getSourceIndex() { return sourceIndex; }
    public void setSourceIndex(String sourceIndex) { this.sourceIndex = sourceIndex; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
