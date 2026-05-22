package com.jiacun.liferecorderserver.controller;

import com.jiacun.liferecorderserver.dto.AttachmentResponse;
import com.jiacun.liferecorderserver.service.LifeChangesService;
import com.jiacun.liferecorderserver.service.LifeIndexService;
import com.jiacun.liferecorderserver.service.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * AI 附件上传控制器
 */
@RestController
@RequestMapping("/ai")
public class AttachmentUploadController {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private LifeIndexService lifeIndexService;

    @Autowired
    private LifeChangesService lifeChangesService;

    // 文件大小限制：图片 10MB，普通文件 20MB
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;   // 20MB

    /**
     * 上传 AI 附件
     */
    @PostMapping(value = "/upload-attachment", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "source", defaultValue = "chat_upload") String source
    ) {
        try {
            // 1. 验证文件
            if (file == null || file.isEmpty()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AttachmentResponse(false, "文件不能为空"));
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AttachmentResponse(false, "文件名不能为空"));
            }

            // 清理文件名（移除非法字符）
            String cleanFilename = sanitizeFilename(originalFilename);
            if (cleanFilename.isEmpty()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AttachmentResponse(false, "文件名无效"));
            }

            // 2. 检查文件大小
            long fileSize = file.getSize();
            String contentType = file.getContentType();
            
            if (contentType != null && contentType.startsWith("image/")) {
                if (fileSize > MAX_IMAGE_SIZE) {
                    return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new AttachmentResponse(false, "图片文件大小不能超过 10MB"));
                }
            } else {
                if (fileSize > MAX_FILE_SIZE) {
                    return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new AttachmentResponse(false, "文件大小不能超过 20MB"));
                }
            }

            // 3. 确定保存路径
            boolean isImage = contentType != null && contentType.startsWith("image/");
            Path targetDir = isImage 
                ? workspaceService.getTodayAiInboxPhotosDir() 
                : workspaceService.getTodayAiInboxFilesDir();
            
            // 处理文件名冲突：如果文件已存在，追加时间戳
            String finalFilename = handleFilenameConflict(targetDir, cleanFilename);
            Path filePath = targetDir.resolve(finalFilename);

            // 安全检查：确保路径在 workspace 内
            workspaceService.validatePath(filePath);

            // 4. 保存文件
            Files.createDirectories(targetDir);
            file.transferTo(filePath.toFile());

            // 5. 生成唯一 ID
            String attachmentId = "att_" + System.currentTimeMillis();

            // 6. 确定类型
            String itemType = isImage ? "image" : "ai_attachment";

            // 7. 构建相对路径
            String relativePath = isImage 
                ? "ai_inbox/photos/" + finalFilename 
                : "ai_inbox/files/" + finalFilename;

            // 8. 构建预览文本
            String preview;
            if (message != null && !message.trim().isEmpty()) {
                preview = "用户发送附件时附带消息：" + message;
            } else {
                preview = "用户在 AI 对话中上传的附件";
            }

            // 9. 更新 index.json
            long now = System.currentTimeMillis();
            LifeIndexService.IndexItem item = new LifeIndexService.IndexItem();
            item.setId(attachmentId);
            item.setType(itemType);
            item.setName(finalFilename);
            item.setRelativePath(relativePath);
            item.setMimeType(contentType != null ? contentType : "application/octet-stream");
            item.setSize(fileSize);
            item.setSource(source);
            item.setPreview(preview);
            item.setCreatedTime(now);
            item.setUpdatedTime(now);
            lifeIndexService.addOrUpdateItem(item);

            // 10. 追加 changes.json
            LifeChangesService.ChangeEntry change = new LifeChangesService.ChangeEntry();
            change.setType("ai_attachment_uploaded");
            change.setTargetId(attachmentId);
            change.setTargetPath(relativePath);
            change.setSource(source);
            change.setDescription("用户上传了 AI 对话附件：" + finalFilename);
            change.setCreatedTime(now);
            lifeChangesService.appendChange(change);

            // 11. 返回成功响应
            AttachmentResponse response = new AttachmentResponse(
                true,
                attachmentId,
                itemType,
                finalFilename,
                relativePath,
                contentType != null ? contentType : "application/octet-stream",
                fileSize,
                now,
                "附件已保存"
            );

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);

        } catch (IOException e) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AttachmentResponse(false, "文件保存失败：" + e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AttachmentResponse(false, "安全错误：" + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AttachmentResponse(false, "上传失败：" + e.getMessage()));
        }
    }

    /**
     * 清理文件名，移除非法字符
     */
    private String sanitizeFilename(String filename) {
        // 移除路径分隔符和控制字符
        String cleaned = filename.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_");
        
        // 移除前后空格
        cleaned = cleaned.trim();
        
        // 如果清理后为空，使用默认名称
        if (cleaned.isEmpty()) {
            return "unnamed_file";
        }
        
        return cleaned;
    }

    /**
     * 处理文件名冲突：如果文件已存在，追加时间戳
     */
    private String handleFilenameConflict(Path directory, String filename) throws IOException {
        Path filePath = directory.resolve(filename);
        
        // 如果文件不存在，直接返回原文件名
        if (!Files.exists(filePath)) {
            return filename;
        }

        // 提取文件名和扩展名
        int dotIndex = filename.lastIndexOf('.');
        String nameWithoutExt;
        String extension;
        
        if (dotIndex > 0) {
            nameWithoutExt = filename.substring(0, dotIndex);
            extension = filename.substring(dotIndex);
        } else {
            nameWithoutExt = filename;
            extension = "";
        }

        // 追加时间戳
        String timestamp = String.valueOf(System.currentTimeMillis());
        String newFilename = nameWithoutExt + "_" + timestamp + extension;

        return newFilename;
    }
}
