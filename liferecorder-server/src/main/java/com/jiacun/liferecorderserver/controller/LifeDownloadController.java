package com.jiacun.liferecorderserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/life")
public class LifeDownloadController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LifeDownloadController.class);

    private static final String WORKSPACE_ROOT = "D:/LifeRecorder";

    // 允许下载的路径前缀（相对路径）
    private static final String[] ALLOWED_PREFIXES = {
        "today/ai_generated/markdown",
        "today/ai_generated/json",
        "today/ai_inbox/files",
        "today/ai_inbox/photos",
        "today/passive/files",
        "today/passive/photos",
        "phone_sync/current/cache/files",
        "phone_sync/current/cache/photos",
        "summaries",
        "today/summaries",
        // 兼容：不带 today/ 前缀
        "ai_generated/markdown",
        "ai_generated/json",
        "ai_inbox/files",
        "ai_inbox/photos"
    };

    /**
     * 下载 LifeRecorder 工作区内的文件
     * @param relativePath 文件相对路径（如 ai_generated/markdown/xxx.md）
     */
    @GetMapping("/download-file")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("relativePath") String relativePath) {
        try {
            // 1. 安全检查：禁止 null 和空白
            if (relativePath == null || relativePath.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // 2. 清理路径：移除开头的 /
            relativePath = relativePath.trim();
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            // 3. 禁止路径穿越：检查是否包含 ..
            if (relativePath.contains("..") || relativePath.contains("~")) {
                return ResponseEntity.badRequest().build();
            }

            // 4. 前缀白名单检查
            boolean allowed = false;
            for (String prefix : ALLOWED_PREFIXES) {
                if (relativePath.startsWith(prefix)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                return ResponseEntity.status(403).build();
            }

            // 5. 构建绝对路径（不带 today/ 前缀时自动补上）
            String resolvedRelativePath = relativePath;
            boolean startsWithoutToday = relativePath.startsWith("ai_generated/")
                || relativePath.startsWith("ai_inbox/")
                || relativePath.startsWith("passive/");
            if (startsWithoutToday) {
                resolvedRelativePath = "today/" + relativePath;
                log.info("[DownloadFile] auto-prepended today/ -> {}", resolvedRelativePath);
            }
            log.info("[DownloadFile] requested relativePath={}", relativePath);
            log.info("[DownloadFile] resolved relativePath={}", resolvedRelativePath);

            String normalizedPath = resolvedRelativePath.replace("/", "\\");
            Path filePath = Paths.get(WORKSPACE_ROOT, normalizedPath);

            // 6. 确保路径在 WORKSPACE_ROOT 内（防止符号链接攻击）
            Path resolvedPath = filePath.toAbsolutePath().normalize();
            Path workspaceRoot = Paths.get(WORKSPACE_ROOT).toAbsolutePath().normalize();
            if (!resolvedPath.startsWith(workspaceRoot)) {
                log.warn("[DownloadFile] path traversal blocked: {}", resolvedPath);
                return ResponseEntity.status(403).build();
            }

            // 7. 检查文件是否存在
            log.info("[DownloadFile] resolved path={}", resolvedPath);
            log.info("[DownloadFile] exists={}", Files.exists(resolvedPath));
            if (!Files.exists(resolvedPath) || !Files.isRegularFile(resolvedPath)) {
                return ResponseEntity.notFound().build();
            }

            // 8. 推断 MIME 类型
            String mimeType = inferMimeType(relativePath);
            String filename = relativePath.substring(relativePath.lastIndexOf('/') + 1);
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.name())
                .replace("+", "%20");

            // 9. 读取文件
            byte[] fileContent = Files.readAllBytes(resolvedPath);

            // 10. 返回
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.setContentDispositionFormData("attachment", encodedFilename);
            headers.setContentLength(fileContent.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);

        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 推断 MIME 类型
     */
    private String inferMimeType(String relativePath) {
        if (relativePath == null) return "application/octet-stream";
        String lower = relativePath.toLowerCase();
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) return "text/markdown; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".txt")) return "text/plain; charset=UTF-8";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".csv")) return "text/csv; charset=UTF-8";
        if (lower.endsWith(".xml")) return "application/xml; charset=UTF-8";
        if (lower.endsWith(".zip")) return "application/zip";
        return "application/octet-stream";
    }
}
