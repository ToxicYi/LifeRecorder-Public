package com.jiacun.liferecorderserver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Pending Requests 响应数据结构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PendingRequestsResponse {
    private int schemaVersion;
    private Long updatedTime;
    private List<PendingRequestItem> requests;

    public PendingRequestsResponse() {}

    public PendingRequestsResponse(int schemaVersion, Long updatedTime, List<PendingRequestItem> requests) {
        this.schemaVersion = schemaVersion;
        this.updatedTime = updatedTime;
        this.requests = requests;
    }

    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }
    
    public Long getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Long updatedTime) { this.updatedTime = updatedTime; }
    
    public List<PendingRequestItem> getRequests() { return requests; }
    public void setRequests(List<PendingRequestItem> requests) { this.requests = requests; }
}
