package com.jiacun.liferecorderserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jiacun.liferecorderserver.dto.AppFilesIndexRequest;
import com.jiacun.liferecorderserver.dto.CreateFetchRequest;
import com.jiacun.liferecorderserver.dto.PendingRequestItem;
import com.jiacun.liferecorderserver.dto.PhoneFileIndexRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 手机文件索引同步服务
 */
@Service
public class PhoneSyncService {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private LifeIndexService lifeIndexService;

    @Autowired
    private LifeChangesService lifeChangesService;

    private final ObjectMapper objectMapper;
    private static final String PHONE_SYNC_DIR = "phone_sync";
    private static final String CURRENT_DIR = "current";
    private static final String FILE_INDEX_FILENAME = "file_index.json";
    private static final String PENDING_REQUESTS_FILENAME = "pending_requests.json";
    private static final String APP_FILES_INDEX_FILENAME = "app_files_index.json";
    private static final String CACHE_DIR = "cache/files";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    public PhoneSyncService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 处理手机文件索引上传
     */
    public Map<String, Object> processFileIndexUpload(PhoneFileIndexRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 确保 phone_sync/current 目录存在
            Path phoneSyncDir = ensurePhoneSyncDirectory();
            
            // 2. 读取旧的 file_index.json（如果存在）
            List<PhoneFileIndexRequest.FileItem> oldFiles = loadOldFileIndex(phoneSyncDir);
            
            // 3. 按 fileId 合并新旧索引
            MergeResult mergeResult = mergeFileIndexes(oldFiles, request.getFiles());
            
            // 4. 保存合并后的 file_index.json
            saveMergedFileIndex(phoneSyncDir, mergeResult.getMergedFiles());
            
            // 5. 检测变化并追加到 changes.json
            appendChangesForUpdates(mergeResult.getUpdatedFileIds(), mergeResult.getNewFileIds());
            
            // 6. 更新 today/index.json
            updateTodayIndex();
            
            // 7. 返回结果
            result.put("success", true);
            result.put("message", "文件索引已保存");
            result.put("totalFiles", mergeResult.getMergedFiles().size());
            result.put("updatedFiles", mergeResult.getUpdatedFileIds().size());
            result.put("newFiles", mergeResult.getNewFileIds().size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "处理失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    /**
     * 确保 phone_sync/current 目录存在
     */
    private Path ensurePhoneSyncDirectory() throws IOException {
        Path rootDir = workspaceService.getRootDir();
        Path phoneSyncPath = rootDir.resolve(PHONE_SYNC_DIR).resolve(CURRENT_DIR).normalize();
        
        // 安全检查
        if (!phoneSyncPath.startsWith(rootDir)) {
            throw new SecurityException("禁止访问 workspace 外的路径");
        }
        
        Files.createDirectories(phoneSyncPath);
        return phoneSyncPath;
    }

    /**
     * 加载旧的文件索引
     */
    private List<PhoneFileIndexRequest.FileItem> loadOldFileIndex(Path phoneSyncDir) {
        try {
            Path indexPath = phoneSyncDir.resolve(FILE_INDEX_FILENAME);
            if (!Files.exists(indexPath)) {
                return new ArrayList<>();
            }
            
            String content = Files.readString(indexPath, StandardCharsets.UTF_8);
            return objectMapper.readValue(content, new TypeReference<List<PhoneFileIndexRequest.FileItem>>() {});
            
        } catch (IOException e) {
            System.err.println("加载旧文件索引失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 合并新旧文件索引
     */
    private MergeResult mergeFileIndexes(List<PhoneFileIndexRequest.FileItem> oldFiles, 
                                        List<PhoneFileIndexRequest.FileItem> newFiles) {
        MergeResult result = new MergeResult();
        
        // 使用 Map 存储，key 为 fileId
        Map<String, PhoneFileIndexRequest.FileItem> mergedMap = new LinkedHashMap<>();
        
        // 先添加所有旧文件
        for (PhoneFileIndexRequest.FileItem item : oldFiles) {
            mergedMap.put(item.getFileId(), item);
        }
        
        Set<String> updatedFileIds = new HashSet<>();
        Set<String> newFileIds = new HashSet<>();
        
        // 处理新文件：覆盖或新增
        for (PhoneFileIndexRequest.FileItem newItem : newFiles) {
            String fileId = newItem.getFileId();
            
            if (mergedMap.containsKey(fileId)) {
                // 检查是否有变化（contentHash 或 lastModified）
                PhoneFileIndexRequest.FileItem oldItem = mergedMap.get(fileId);
                boolean hasChanged = !Objects.equals(oldItem.getContentHash(), newItem.getContentHash()) ||
                                   !Objects.equals(oldItem.getLastModified(), newItem.getLastModified());
                
                if (hasChanged) {
                    updatedFileIds.add(fileId);
                }
                
                // 覆盖旧记录
                mergedMap.put(fileId, newItem);
            } else {
                // 新增记录
                newFileIds.add(fileId);
                mergedMap.put(fileId, newItem);
            }
        }
        
        result.setMergedFiles(new ArrayList<>(mergedMap.values()));
        result.setUpdatedFileIds(updatedFileIds);
        result.setNewFileIds(newFileIds);
        
        return result;
    }

    /**
     * 保存合并后的文件索引
     */
    private void saveMergedFileIndex(Path phoneSyncDir, List<PhoneFileIndexRequest.FileItem> files) 
            throws IOException {
        Path indexPath = phoneSyncDir.resolve(FILE_INDEX_FILENAME);
        String jsonContent = objectMapper.writeValueAsString(files);
        Files.writeString(indexPath, jsonContent, StandardCharsets.UTF_8);
    }

    /**
     * 追加变化记录到 changes.json
     */
    private void appendChangesForUpdates(Set<String> updatedFileIds, Set<String> newFileIds) {
        long now = System.currentTimeMillis();
        
        // 为每个更新的文件添加 change 记录
        for (String fileId : updatedFileIds) {
            LifeChangesService.ChangeEntry change = new LifeChangesService.ChangeEntry();
            change.setType("phone_file_index_updated");
            change.setTargetId(fileId);
            change.setDescription("手机文件索引发生更新: " + fileId);
            change.setCreatedTime(now);
            lifeChangesService.appendChange(change);
        }
        
        // 为每个新增的文件添加 change 记录
        for (String fileId : newFileIds) {
            LifeChangesService.ChangeEntry change = new LifeChangesService.ChangeEntry();
            change.setType("phone_file_index_updated");
            change.setTargetId(fileId);
            change.setDescription("手机文件索引新增: " + fileId);
            change.setCreatedTime(now);
            lifeChangesService.appendChange(change);
        }
    }

    /**
     * 更新 today/index.json，添加 phone_file_index 项
     */
    private void updateTodayIndex() {
        long now = System.currentTimeMillis();
        
        LifeIndexService.IndexItem item = new LifeIndexService.IndexItem();
        item.setId("phone_file_index");
        item.setType("file");
        item.setName(FILE_INDEX_FILENAME);
        item.setRelativePath("../" + PHONE_SYNC_DIR + "/" + CURRENT_DIR + "/" + FILE_INDEX_FILENAME);
        item.setMimeType("application/json");
        item.setSource("android_app");
        item.setCreatedTime(now);
        item.setUpdatedTime(now);
        
        lifeIndexService.addOrUpdateItem(item);
    }

    /**
     * 获取 App 文件索引
     */
    public Map<String, Object> getAppFilesIndex() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Path phoneSyncDir = ensurePhoneSyncDirectory();
            Path indexPath = phoneSyncDir.resolve(APP_FILES_INDEX_FILENAME);
            
            if (!Files.exists(indexPath)) {
                // 文件不存在，返回空列表
                result.put("schemaVersion", 1);
                result.put("deviceId", "android_main");
                result.put("updatedTime", System.currentTimeMillis());
                result.put("files", new ArrayList<>());
                return result;
            }
            
            // 读取并返回文件内容
            String content = Files.readString(indexPath, StandardCharsets.UTF_8);
            List<AppFilesIndexRequest.AppFileItem> files = objectMapper.readValue(
                content,
                new TypeReference<List<AppFilesIndexRequest.AppFileItem>>() {}
            );
            
            result.put("schemaVersion", 1);
            result.put("deviceId", "android_main");
            result.put("updatedTime", System.currentTimeMillis());
            result.put("files", files);
            
        } catch (IOException e) {
            System.err.println("读取 app_files_index.json 失败: " + e.getMessage());
            result.put("schemaVersion", 1);
            result.put("deviceId", "android_main");
            result.put("updatedTime", System.currentTimeMillis());
            result.put("files", new ArrayList<>());
        }
        
        return result;
    }

    /**
     * 上传 App 文件索引
     */
    public Map<String, Object> uploadAppFilesIndex(AppFilesIndexRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 确保 phone_sync/current 目录存在
            Path phoneSyncDir = ensurePhoneSyncDirectory();
            
            // 2. 读取旧的 app_files_index.json（如果存在）
            List<AppFilesIndexRequest.AppFileItem> oldFiles = loadOldAppFilesIndex(phoneSyncDir);
            
            // 3. 按 fileId 合并新旧索引
            AppFilesMergeResult mergeResult = mergeAppFilesIndexes(oldFiles, request.getFiles());
            
            // 4. 保存合并后的 app_files_index.json
            saveMergedAppFilesIndex(phoneSyncDir, mergeResult.getMergedFiles());
            
            // 5. 检测变化并追加到 changes.json
            appendAppFilesChangesForUpdates(mergeResult.getUpdatedFileIds(), mergeResult.getNewFileIds());
            
            // 6. 更新 today/index.json
            updateTodayIndexForAppFiles();
            
            // 7. 返回结果
            result.put("success", true);
            result.put("message", "App 文件索引已保存");
            result.put("totalFiles", mergeResult.getMergedFiles().size());
            result.put("updatedFiles", mergeResult.getUpdatedFileIds().size());
            result.put("newFiles", mergeResult.getNewFileIds().size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "处理失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    /**
     * 加载旧的 App 文件索引
     */
    private List<AppFilesIndexRequest.AppFileItem> loadOldAppFilesIndex(Path phoneSyncDir) {
        try {
            Path indexPath = phoneSyncDir.resolve(APP_FILES_INDEX_FILENAME);
            if (!Files.exists(indexPath)) {
                return new ArrayList<>();
            }
            
            String content = Files.readString(indexPath, StandardCharsets.UTF_8);
            return objectMapper.readValue(content, new TypeReference<List<AppFilesIndexRequest.AppFileItem>>() {});
            
        } catch (IOException e) {
            System.err.println("加载旧 App 文件索引失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 合并新旧 App 文件索引
     */
    private AppFilesMergeResult mergeAppFilesIndexes(List<AppFilesIndexRequest.AppFileItem> oldFiles, 
                                                     List<AppFilesIndexRequest.AppFileItem> newFiles) {
        AppFilesMergeResult result = new AppFilesMergeResult();
        
        // 使用 Map 存储，key 为 fileId
        Map<String, AppFilesIndexRequest.AppFileItem> mergedMap = new LinkedHashMap<>();
        
        // 先添加所有旧文件
        for (AppFilesIndexRequest.AppFileItem item : oldFiles) {
            mergedMap.put(item.getFileId(), item);
        }
        
        Set<String> updatedFileIds = new HashSet<>();
        Set<String> newFileIds = new HashSet<>();
        
        // 处理新文件：覆盖或新增
        for (AppFilesIndexRequest.AppFileItem newItem : newFiles) {
            String fileId = newItem.getFileId();
            
            if (mergedMap.containsKey(fileId)) {
                // 检查是否有变化（contentHash 或 lastModified）
                AppFilesIndexRequest.AppFileItem oldItem = mergedMap.get(fileId);
                boolean hasChanged = !Objects.equals(oldItem.getContentHash(), newItem.getContentHash()) ||
                                   !Objects.equals(oldItem.getLastModified(), newItem.getLastModified());
                
                if (hasChanged) {
                    updatedFileIds.add(fileId);
                }
                
                // 覆盖旧记录
                mergedMap.put(fileId, newItem);
            } else {
                // 新增记录
                newFileIds.add(fileId);
                mergedMap.put(fileId, newItem);
            }
        }
        
        result.setMergedFiles(new ArrayList<>(mergedMap.values()));
        result.setUpdatedFileIds(updatedFileIds);
        result.setNewFileIds(newFileIds);
        
        return result;
    }

    /**
     * 保存合并后的 App 文件索引
     */
    private void saveMergedAppFilesIndex(Path phoneSyncDir, List<AppFilesIndexRequest.AppFileItem> files) 
            throws IOException {
        Path indexPath = phoneSyncDir.resolve(APP_FILES_INDEX_FILENAME);
        String jsonContent = objectMapper.writeValueAsString(files);
        Files.writeString(indexPath, jsonContent, StandardCharsets.UTF_8);
    }

    /**
     * 追加 App 文件索引变化记录
     */
    private void appendAppFilesChangesForUpdates(Set<String> updatedFileIds, Set<String> newFileIds) {
        long now = System.currentTimeMillis();
        
        // 为每个更新的文件添加 change 记录
        for (String fileId : updatedFileIds) {
            LifeChangesService.ChangeEntry change = new LifeChangesService.ChangeEntry();
            change.setType("app_file_index_updated");
            change.setTargetId(fileId);
            change.setSource("android_app");
            change.setDescription("LifeRecorder App 文件索引发生更新: " + fileId);
            change.setCreatedTime(now);
            lifeChangesService.appendChange(change);
        }
        
        // 为每个新增的文件添加 change 记录
        for (String fileId : newFileIds) {
            LifeChangesService.ChangeEntry change = new LifeChangesService.ChangeEntry();
            change.setType("app_file_index_updated");
            change.setTargetId(fileId);
            change.setSource("android_app");
            change.setDescription("LifeRecorder App 文件索引新增: " + fileId);
            change.setCreatedTime(now);
            lifeChangesService.appendChange(change);
        }
    }

    /**
     * 更新 today/index.json，添加 app_files_index 项
     */
    private void updateTodayIndexForAppFiles() {
        long now = System.currentTimeMillis();
        
        LifeIndexService.IndexItem item = new LifeIndexService.IndexItem();
        item.setId("app_files_index");
        item.setType("phone_file_index");
        item.setName(APP_FILES_INDEX_FILENAME);
        item.setRelativePath("../" + PHONE_SYNC_DIR + "/" + CURRENT_DIR + "/" + APP_FILES_INDEX_FILENAME);
        item.setMimeType("application/json");
        item.setSource("android_app");
        item.setCreatedTime(now);
        item.setUpdatedTime(now);
        
        lifeIndexService.addOrUpdateItem(item);
    }

    /**
     * 上传被请求的文件
     */
    public Map<String, Object> uploadRequestedFile(String requestId, String deviceId, 
                                                   String fileId, MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 验证参数
            if (requestId == null || deviceId == null || fileId == null || file == null) {
                result.put("success", false);
                result.put("message", "缺少必填参数");
                return result;
            }
            
            // 2. 验证文件大小
            if (file.getSize() > MAX_FILE_SIZE) {
                result.put("success", false);
                result.put("message", "文件大小超过限制（最大50MB）");
                return result;
            }
            
            Path phoneSyncDir = ensurePhoneSyncDirectory();
            
            // 3. 读取 pending_requests.json
            List<PendingRequestItem> allRequests = loadPendingRequests(phoneSyncDir);
            
            // 4. 找到对应的 request
            Optional<PendingRequestItem> targetRequestOpt = allRequests.stream()
                .filter(req -> requestId.equals(req.getId()) &&
                             deviceId.equals(req.getDeviceId()) &&
                             fileId.equals(req.getFileId()) &&
                             ("pending".equals(req.getStatus()) || "uploading".equals(req.getStatus())))
                .findFirst();
            
            if (!targetRequestOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "未找到匹配的待处理请求");
                return result;
            }
            
            PendingRequestItem targetRequest = targetRequestOpt.get();
            
            // 5. 清理文件名，防止路径穿越
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                result.put("success", false);
                result.put("message", "文件名无效");
                return result;
            }
            
            String safeFilename = sanitizeFilename(originalFilename);
            
            // 6. 保存文件到 cache/files/{fileId}/{safeFilename}
            Path cacheDir = phoneSyncDir.resolve(CACHE_DIR).resolve(fileId).normalize();
            
            // 安全检查：确保路径在 workspace 内
            Path rootDir = workspaceService.getRootDir();
            if (!cacheDir.startsWith(rootDir)) {
                result.put("success", false);
                result.put("message", "非法路径访问");
                return result;
            }
            
            Files.createDirectories(cacheDir);
            
            Path filePath = cacheDir.resolve(safeFilename).normalize();
            if (!filePath.startsWith(rootDir)) {
                result.put("success", false);
                result.put("message", "非法文件路径");
                return result;
            }
            
            // 保存文件（覆盖同名文件）
            file.transferTo(filePath.toFile());
            
            // 7. 更新 pending_requests.json
            String cachedPath = PHONE_SYNC_DIR + "/" + CURRENT_DIR + "/" + CACHE_DIR + "/" + fileId + "/" + safeFilename;
            targetRequest.setStatus("completed");
            targetRequest.setResultCachedPath(cachedPath);
            targetRequest.setUpdatedTime(System.currentTimeMillis());
            targetRequest.setError(null);
            
            savePendingRequests(phoneSyncDir, allRequests);
            
            // 8. 更新 file_index.json
            updateFileIndexAfterUpload(fileId, cachedPath);
            
            // 8.1 更新 app_files_index.json
            updateAppFilesIndexAfterUpload(fileId, cachedPath);
            
            // 9. 追加 changes.json
            appendFileCachedChange(fileId, cachedPath);
            
            result.put("success", true);
            result.put("requestId", requestId);
            result.put("fileId", fileId);
            result.put("cachedPath", cachedPath);
            result.put("message", "文件已上传并缓存");
            
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "文件保存失败: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "处理失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    /**
     * 清理文件名，防止路径穿越和非法字符
     */
    private String sanitizeFilename(String filename) {
        // 移除路径分隔符
        String sanitized = filename.replace("/", "_").replace("\\", "_");
        
        // 移除 ..
        sanitized = sanitized.replace("..", "_");
        
        // 只保留安全字符
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
        
        // 限制长度
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }
        
        return sanitized;
    }

    /**
     * 更新 file_index.json 中的文件信息
     */
    private void updateFileIndexAfterUpload(String fileId, String cachedPath) {
        try {
            Path phoneSyncDir = ensurePhoneSyncDirectory();
            Path indexPath = phoneSyncDir.resolve(FILE_INDEX_FILENAME);
            
            if (!Files.exists(indexPath)) {
                System.err.println("file_index.json 不存在，跳过更新");
                return;
            }
            
            String content = Files.readString(indexPath, StandardCharsets.UTF_8);
            List<PhoneFileIndexRequest.FileItem> files = objectMapper.readValue(
                content, 
                new TypeReference<List<PhoneFileIndexRequest.FileItem>>() {}
            );
            
            // 查找并更新对应的 fileId
            boolean found = false;
            for (PhoneFileIndexRequest.FileItem item : files) {
                if (fileId.equals(item.getFileId())) {
                    item.setAvailableLocally(true);
                    item.setCachedPath(cachedPath);
                    found = true;
                    break;
                }
            }
            
            if (found) {
                String jsonContent = objectMapper.writeValueAsString(files);
                Files.writeString(indexPath, jsonContent, StandardCharsets.UTF_8);
            }
            
        } catch (IOException e) {
            System.err.println("更新 file_index.json 失败: " + e.getMessage());
        }
    }

    /**
     * 更新 app_files_index.json 中的文件信息
     */
    private void updateAppFilesIndexAfterUpload(String fileId, String cachedPath) {
        try {
            Path phoneSyncDir = ensurePhoneSyncDirectory();
            Path indexPath = phoneSyncDir.resolve(APP_FILES_INDEX_FILENAME);
            
            if (!Files.exists(indexPath)) {
                System.err.println("app_files_index.json 不存在，跳过更新");
                return;
            }
            
            String content = Files.readString(indexPath, StandardCharsets.UTF_8);
            List<AppFilesIndexRequest.AppFileItem> files = objectMapper.readValue(
                content,
                new TypeReference<List<AppFilesIndexRequest.AppFileItem>>() {}
            );
            
            long now = System.currentTimeMillis();
            boolean found = false;
            
            // 查找并更新对应的 fileId 或 linkedPhoneFileId
            for (AppFilesIndexRequest.AppFileItem item : files) {
                // 匹配 fileId 或 linkedPhoneFileId
                if (fileId.equals(item.getFileId()) || fileId.equals(item.getLinkedPhoneFileId())) {
                    item.setAvailableLocally(true);
                    item.setCachedPath(cachedPath);
                    found = true;
                    // 不覆盖其他字段（name, virtualPath, mimeType, source 等）
                }
            }
            
            if (found) {
                String jsonContent = objectMapper.writeValueAsString(files);
                Files.writeString(indexPath, jsonContent, StandardCharsets.UTF_8);
            }
            
        } catch (IOException e) {
            System.err.println("更新 app_files_index.json 失败: " + e.getMessage());
        }
    }

    /**
     * 追加 file_cached 变化记录
     */
    private void appendFileCachedChange(String fileId, String cachedPath) {
        long now = System.currentTimeMillis();
        
        LifeChangesService.ChangeEntry change = new LifeChangesService.ChangeEntry();
        change.setType("phone_file_cached");
        change.setTargetId(fileId);
        change.setTargetPath(cachedPath);
        change.setSource("android_app");
        change.setDescription("手机端按需上传了指定文件: " + fileId);
        change.setCreatedTime(now);
        lifeChangesService.appendChange(change);
    }

    /**
     * 获取 pending requests
     */
    public List<PendingRequestItem> getPendingRequests() {
        try {
            Path phoneSyncDir = ensurePhoneSyncDirectory();
            List<PendingRequestItem> allRequests = loadPendingRequests(phoneSyncDir);
            
            // 只返回 status = pending 或 uploading 的请求
            return allRequests.stream()
                .filter(req -> "pending".equals(req.getStatus()) || "uploading".equals(req.getStatus()))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            System.err.println("获取 pending requests 失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 创建 fetch request
     */
    public Map<String, Object> createFetchRequest(CreateFetchRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证参数
            if (request.getDeviceId() == null || request.getFileId() == null) {
                result.put("success", false);
                result.put("message", "deviceId 和 fileId 不能为空");
                return result;
            }
            
            Path phoneSyncDir = ensurePhoneSyncDirectory();
            List<PendingRequestItem> allRequests = loadPendingRequests(phoneSyncDir);
            
            // 检查是否已存在同 deviceId + fileId + status=pending 的请求
            Optional<PendingRequestItem> existingRequest = allRequests.stream()
                .filter(req -> request.getDeviceId().equals(req.getDeviceId()) &&
                             request.getFileId().equals(req.getFileId()) &&
                             "pending".equals(req.getStatus()))
                .findFirst();
            
            if (existingRequest.isPresent()) {
                // 返回旧请求
                result.put("success", true);
                result.put("message", "请求已存在");
                result.put("request", existingRequest.get());
                result.put("isExisting", true);
                return result;
            }
            
            // 创建新请求
            long now = System.currentTimeMillis();
            PendingRequestItem newRequest = new PendingRequestItem();
            newRequest.setId("request_" + now);
            newRequest.setType("fetch_phone_file");
            newRequest.setStatus("pending");
            newRequest.setDeviceId(request.getDeviceId());
            newRequest.setFileId(request.getFileId());
            newRequest.setSourceIndex(request.getSourceIndex());
            newRequest.setReason(request.getReason());
            newRequest.setRequestedBy("spring_boot");
            newRequest.setCreatedTime(now);
            newRequest.setUpdatedTime(now);
            newRequest.setResultCachedPath(null);
            newRequest.setError(null);
            
            allRequests.add(newRequest);
            
            // 保存
            savePendingRequests(phoneSyncDir, allRequests);
            
            // 追加 changes.json
            appendFetchRequestedChange(request.getFileId());
            
            result.put("success", true);
            result.put("message", "请求已创建");
            result.put("request", newRequest);
            result.put("isExisting", false);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建请求失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    /**
     * 加载 pending requests
     */
    private List<PendingRequestItem> loadPendingRequests(Path phoneSyncDir) {
        try {
            Path requestsPath = phoneSyncDir.resolve(PENDING_REQUESTS_FILENAME);
            if (!Files.exists(requestsPath)) {
                return new ArrayList<>();
            }
            
            String content = Files.readString(requestsPath, StandardCharsets.UTF_8);
            return objectMapper.readValue(content, new TypeReference<List<PendingRequestItem>>() {});
            
        } catch (IOException e) {
            System.err.println("加载 pending requests 失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 保存 pending requests
     */
    private void savePendingRequests(Path phoneSyncDir, List<PendingRequestItem> requests) 
            throws IOException {
        Path requestsPath = phoneSyncDir.resolve(PENDING_REQUESTS_FILENAME);
        String jsonContent = objectMapper.writeValueAsString(requests);
        Files.writeString(requestsPath, jsonContent, StandardCharsets.UTF_8);
    }

    /**
     * 追加 fetch requested 变化记录
     */
    private void appendFetchRequestedChange(String fileId) {
        long now = System.currentTimeMillis();
        
        LifeChangesService.ChangeEntry change = new LifeChangesService.ChangeEntry();
        change.setType("phone_file_fetch_requested");
        change.setTargetId(fileId);
        change.setSource("spring_boot");
        change.setDescription("请求手机上传指定文件: " + fileId);
        change.setCreatedTime(now);
        lifeChangesService.appendChange(change);
    }

    /**
     * App 文件索引合并结果内部类
     */
    private static class AppFilesMergeResult {
        private List<AppFilesIndexRequest.AppFileItem> mergedFiles;
        private Set<String> updatedFileIds;
        private Set<String> newFileIds;

        public List<AppFilesIndexRequest.AppFileItem> getMergedFiles() { return mergedFiles; }
        public void setMergedFiles(List<AppFilesIndexRequest.AppFileItem> mergedFiles) { 
            this.mergedFiles = mergedFiles; 
        }
        
        public Set<String> getUpdatedFileIds() { return updatedFileIds; }
        public void setUpdatedFileIds(Set<String> updatedFileIds) { 
            this.updatedFileIds = updatedFileIds; 
        }
        
        public Set<String> getNewFileIds() { return newFileIds; }
        public void setNewFileIds(Set<String> newFileIds) { 
            this.newFileIds = newFileIds; 
        }
    }

    /**
     * 合并结果内部类
     */
    private static class MergeResult {
        private List<PhoneFileIndexRequest.FileItem> mergedFiles;
        private Set<String> updatedFileIds;
        private Set<String> newFileIds;

        public List<PhoneFileIndexRequest.FileItem> getMergedFiles() { return mergedFiles; }
        public void setMergedFiles(List<PhoneFileIndexRequest.FileItem> mergedFiles) { 
            this.mergedFiles = mergedFiles; 
        }
        
        public Set<String> getUpdatedFileIds() { return updatedFileIds; }
        public void setUpdatedFileIds(Set<String> updatedFileIds) { 
            this.updatedFileIds = updatedFileIds; 
        }
        
        public Set<String> getNewFileIds() { return newFileIds; }
        public void setNewFileIds(Set<String> newFileIds) { 
            this.newFileIds = newFileIds; 
        }
    }
}
