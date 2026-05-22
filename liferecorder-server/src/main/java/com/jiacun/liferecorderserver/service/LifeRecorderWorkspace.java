package com.jiacun.liferecorderserver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class LifeRecorderWorkspace {

    @Value("${liferecorder.workspace:D:/LifeRecorder}")
    private String workspacePath;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 验证日期格式并返回 LocalDate
     */
    public LocalDate validateDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDate.now();
        }
        
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式错误，必须是 yyyy-MM-dd 格式");
        }
    }

    /**
     * 获取指定日期的工作目录路径
     */
    public Path getDayDirectory(LocalDate date) {
        Path workspace = Paths.get(workspacePath).normalize();
        Path dayDir = workspace.resolve("days").resolve(date.format(DATE_FORMATTER)).normalize();
        
        // 安全检查：确保路径在 workspace 内
        if (!dayDir.startsWith(workspace)) {
            throw new SecurityException("非法路径访问");
        }
        
        return dayDir;
    }

    /**
     * 获取 ai_generated 目录路径
     */
    public Path getAiGeneratedDirectory(LocalDate date) {
        Path dayDir = getDayDirectory(date);
        Path aiGenDir = dayDir.resolve("ai_generated").normalize();
        
        // 安全检查
        Path workspace = Paths.get(workspacePath).normalize();
        if (!aiGenDir.startsWith(workspace)) {
            throw new SecurityException("非法路径访问");
        }
        
        return aiGenDir;
    }

    /**
     * 验证文件路径是否安全（在 workspace 内）
     */
    public void validateFilePath(Path filePath) {
        Path workspace = Paths.get(workspacePath).normalize();
        Path normalizedPath = filePath.normalize();
        
        if (!normalizedPath.startsWith(workspace)) {
            throw new SecurityException("禁止访问 workspace 外的路径");
        }
    }

    /**
     * 获取 workspace 根路径
     */
    public Path getWorkspaceRoot() {
        return Paths.get(workspacePath).normalize();
    }
}
