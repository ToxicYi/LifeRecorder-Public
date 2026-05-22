package com.jiacun.liferecorderserver.controller;

import com.jiacun.liferecorderserver.dto.AppFilesIndexRequest;
import com.jiacun.liferecorderserver.dto.CreateFetchRequest;
import com.jiacun.liferecorderserver.dto.PendingRequestItem;
import com.jiacun.liferecorderserver.dto.PendingRequestsResponse;
import com.jiacun.liferecorderserver.dto.PhoneFileIndexRequest;
import com.jiacun.liferecorderserver.dto.PhoneFileIndexResponse;
import com.jiacun.liferecorderserver.dto.UploadRequestedFileResponse;
import com.jiacun.liferecorderserver.service.PhoneSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 手机文件同步控制器 - Phone Sync API
 * 
 * 主要职责：
 * 1. 接收 Android App 上传的文件索引（file_index.json）
 * 2. 管理 Agent 的文件请求队列（pending_requests.json）
 * 3. 接收 App 上传的被请求文件
 * 4. 提供文件索引查询接口
 * 
 * 工作流程：
 * 1. App 启动时上传文件索引 → /upload-file-index
 * 2. Agent 需要手机文件时创建请求 → /create-fetch-request
 * 3. App 定期检查 pending requests → /pending-requests
 * 4. App 上传被请求的文件 → /upload-requested-file
 * 5. Agent 读取缓存的文件内容
 * 
 * 数据存储位置：
 * D:/LifeRecorder/phone_sync/current/
 *   - file_index.json: 手机文件索引
 *   - app_files_index.json: App 应用文件索引
 *   - pending_requests.json: Agent 文件请求队列
 */
@RestController
@RequestMapping("/phone-sync")
public class PhoneSyncController {

    /**
     * 手机同步服务，处理文件索引和请求队列的业务逻辑
     */
    @Autowired
    private PhoneSyncService phoneSyncService;

    /**
     * 上传手机文件索引
     * 
     * Android App 启动或文件变化时调用此接口，上传手机文件列表。
     * 后端会更新 file_index.json，采用 latest_only 策略（相同 fileId 覆盖旧版本）。
     * 
     * @param request 文件索引请求，包含设备 ID 和文件列表
     * @return 响应，包含成功状态、消息、文件统计信息
     */
    @PostMapping("/upload-file-index")
    public PhoneFileIndexResponse uploadFileIndex(@RequestBody PhoneFileIndexRequest request) {
        try {
            // 验证请求参数
            if (request.getFiles() == null || request.getFiles().isEmpty()) {
                return new PhoneFileIndexResponse(false, "文件列表不能为空");
            }

            // 处理文件索引上传
            Map<String, Object> result = phoneSyncService.processFileIndexUpload(request);
            
            boolean success = (Boolean) result.get("success");
            String message = (String) result.get("message");
            int totalFiles = (Integer) result.getOrDefault("totalFiles", 0);
            int updatedFiles = (Integer) result.getOrDefault("updatedFiles", 0);
            int newFiles = (Integer) result.getOrDefault("newFiles", 0);
            
            return new PhoneFileIndexResponse(success, message, totalFiles, updatedFiles, newFiles);
            
        } catch (Exception e) {
            return new PhoneFileIndexResponse(false, "服务器错误: " + e.getMessage());
        }
    }

    /**
     * 获取待处理的文件请求列表
     * 
     * Android App 定期调用此接口，检查是否有 Agent 请求的文件需要上传。
     * 返回 pending_requests.json 中的所有请求。
     * 
     * @return 待处理请求列表响应
     */
    @GetMapping("/pending-requests")
    public PendingRequestsResponse getPendingRequests() {
        try {
            List<PendingRequestItem> requests = phoneSyncService.getPendingRequests();
            
            PendingRequestsResponse response = new PendingRequestsResponse();
            response.setSchemaVersion(1);
            response.setUpdatedTime(System.currentTimeMillis());
            response.setRequests(requests);
            
            return response;
            
        } catch (Exception e) {
            PendingRequestsResponse response = new PendingRequestsResponse();
            response.setSchemaVersion(1);
            response.setUpdatedTime(System.currentTimeMillis());
            response.setRequests(List.of());
            return response;
        }
    }

    /**
     * 获取 App 文件索引
     * 
     * 返回 app_files_index.json 的内容，包含 App 相关的文件信息。
     * 
     * @return App 文件索引数据
     */
    @GetMapping("/app-files-index")
    public Map<String, Object> getAppFilesIndex() {
        try {
            return phoneSyncService.getAppFilesIndex();
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("schemaVersion", 1);
            error.put("deviceId", "android_main");
            error.put("updatedTime", System.currentTimeMillis());
            error.put("files", List.of());
            return error;
        }
    }

    /**
     * 创建文件获取请求
     * 
     * OpenClaw Agent 需要读取手机文件时调用此接口，创建一个新的 fetch request。
     * 请求会被添加到 pending_requests.json 中，等待 App 处理。
     * 
     * @param request 创建请求参数，包含 fileId、reason 等信息
     * @return 创建结果，包含成功状态和 requestId
     */
    @PostMapping("/create-fetch-request")
    public Map<String, Object> createFetchRequest(@RequestBody CreateFetchRequest request) {
        try {
            return phoneSyncService.createFetchRequest(request);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "服务器错误: " + e.getMessage()
            );
            return error;
        }
    }

    /**
     * 上传被请求的文件
     * 
     * Android App 处理 pending request 后，调用此接口上传真实的文件内容。
     * 文件会被保存到 D:/LifeRecorder/phone_sync/cache/ 目录，并更新索引。
     * 
     * @param requestId 请求 ID
     * @param deviceId 设备 ID
     * @param fileId 文件 ID
     * @param file 上传的文件内容
     * @return 上传结果，包含缓存路径等信息
     */
    @PostMapping(value = "/upload-requested-file", consumes = "multipart/form-data")
    public UploadRequestedFileResponse uploadRequestedFile(
            @RequestParam("requestId") String requestId,
            @RequestParam("deviceId") String deviceId,
            @RequestParam("fileId") String fileId,
            @RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = phoneSyncService.uploadRequestedFile(requestId, deviceId, fileId, file);
            
            boolean success = (Boolean) result.get("success");
            String message = (String) result.get("message");
            
            if (!success) {
                return new UploadRequestedFileResponse(false, message);
            }
            
            String reqId = (String) result.get("requestId");
            String fId = (String) result.get("fileId");
            String cachedPath = (String) result.get("cachedPath");
            
            return new UploadRequestedFileResponse(true, reqId, fId, cachedPath, message);
            
        } catch (Exception e) {
            return new UploadRequestedFileResponse(false, "服务器错误: " + e.getMessage());
        }
    }

    /**
     * 上传 App 文件索引
     * 
     * Android App 上传应用相关的文件索引（如聊天记录、笔记等）。
     * 与 upload-file-index 类似，但专门用于 App 生成的文件。
     * 
     * @param request App 文件索引请求
     * @return 响应，包含成功状态和统计信息
     */
    @PostMapping("/upload-app-files-index")
    public PhoneFileIndexResponse uploadAppFilesIndex(@RequestBody AppFilesIndexRequest request) {
        try {
            // 验证请求参数
            if (request.getFiles() == null || request.getFiles().isEmpty()) {
                return new PhoneFileIndexResponse(false, "文件列表不能为空");
            }

            // 处理 App 文件索引上传
            Map<String, Object> result = phoneSyncService.uploadAppFilesIndex(request);
            
            boolean success = (Boolean) result.get("success");
            String message = (String) result.get("message");
            int totalFiles = (Integer) result.getOrDefault("totalFiles", 0);
            int updatedFiles = (Integer) result.getOrDefault("updatedFiles", 0);
            int newFiles = (Integer) result.getOrDefault("newFiles", 0);
            
            return new PhoneFileIndexResponse(success, message, totalFiles, updatedFiles, newFiles);
            
        } catch (Exception e) {
            return new PhoneFileIndexResponse(false, "服务器错误: " + e.getMessage());
        }
    }
}
