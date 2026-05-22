package com.jiacun.liferecorderserver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 手机文件索引上传请求
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhoneFileIndexRequest {
    private int schemaVersion;
    private String deviceId;
    private Long updatedTime;
    private List<FileItem> files;

    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public Long getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Long updatedTime) { this.updatedTime = updatedTime; }
    
    public List<FileItem> getFiles() { return files; }
    public void setFiles(List<FileItem> files) { this.files = files; }

    /**
     * 文件项数据结构
     */
    public static class FileItem {
        private String fileId;
        private String name;
        private String relativePhonePath;
        private String mimeType;
        private long size;
        private Long lastModified;
        private String contentHash;
        private boolean availableLocally;
        private String cachedPath;

        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getRelativePhonePath() { return relativePhonePath; }
        public void setRelativePhonePath(String relativePhonePath) { this.relativePhonePath = relativePhonePath; }
        
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public Long getLastModified() { return lastModified; }
        public void setLastModified(Long lastModified) { this.lastModified = lastModified; }
        
        public String getContentHash() { return contentHash; }
        public void setContentHash(String contentHash) { this.contentHash = contentHash; }
        
        public boolean isAvailableLocally() { return availableLocally; }
        public void setAvailableLocally(boolean availableLocally) { this.availableLocally = availableLocally; }
        
        public String getCachedPath() { return cachedPath; }
        public void setCachedPath(String cachedPath) { this.cachedPath = cachedPath; }
    }
}
