package com.jiacun.liferecorderserver.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * LifeRecorder 工作区路径管理服务
 * 
 * 主要职责：
 * 1. 管理工作区根目录 D:/LifeRecorder 的所有子目录
 * 2. 提供统一的路径访问接口
 * 3. 确保目录结构存在
 * 4. 提供路径安全检查（防止路径穿越攻击）
 * 
 * 工作区目录结构：
 * D:/LifeRecorder/
 *   ├── today/                # 当天数据
 *   │   ├── passive/          # 被动采集的数据
 *   │   │   ├── notes/        # 笔记
 *   │   │   ├── files/        # 文件
 *   │   │   └── photos/       # 照片
 *   │   ├── ai_inbox/         # AI 输入箱
 *   │   │   ├── files/        # 用户上传的附件
 *   │   │   └── photos/       # 用户上传的照片
 *   │   ├── ai_generated/     # Agent 生成的文件
 *   │   │   ├── markdown/     # Markdown 文件
 *   │   │   └── json/         # JSON 文件
 *   │   ├── context/          # 上下文数据
 *   │   │   └── daily_context.json
 *   │   ├── chat/             # 聊天记录
 *   │   │   └── chat_history.json
 *   │   ├── index.json        # 文件索引
 *   │   └── changes.json      # 变更日志
 *   ├── yesterday/            # 昨天数据（归档）
 *   ├── summaries/            # 总结文件
 *   ├── history/              # 历史数据
 *   ├── tasks/                # 任务文件
 *   ├── memory/               # 记忆数据
 *   ├── config/               # 配置文件
 *   └── phone_sync/           # 手机同步数据
 *       ├── current/          # 当前同步状态
 *       └── cache/            # 缓存的文件
 */
@Component
public class WorkspaceService {

    /**
     * 工作区根目录路径 D:/LifeRecorder
     */
    private static final String WORKSPACE_ROOT = "D:/LifeRecorder";
    
    /**
     * 工作区根目录 Path 对象
     */
    private Path rootDir;

    /**
     * Spring Bean 初始化时调用，创建工作区目录结构
     */
    @PostConstruct
    public void init() {
        this.rootDir = Paths.get(WORKSPACE_ROOT).normalize();
        ensureDirectoriesExist();
    }

    /**
     * 确保所有必需的目录存在
     * 如果目录不存在则创建
     */
    private void ensureDirectoriesExist() {
        try {
            Files.createDirectories(getTodayPassiveNotesDir());
            Files.createDirectories(getTodayPassiveFilesDir());
            Files.createDirectories(getTodayPassivePhotosDir());
            Files.createDirectories(getTodayAiInboxFilesDir());
            Files.createDirectories(getTodayAiInboxPhotosDir());
            Files.createDirectories(getTodayAiGeneratedMarkdownDir());
            Files.createDirectories(getTodayAiGeneratedJsonDir());
            Files.createDirectories(getTodayContextDir());
            Files.createDirectories(getTodayChatDir());
            Files.createDirectories(getYesterdayDir());
            Files.createDirectories(getSummariesDir());
            Files.createDirectories(getHistoryDir());
            Files.createDirectories(getTasksDir());
            Files.createDirectories(getMemoryDir());
            Files.createDirectories(getConfigDir());
        } catch (IOException e) {
            System.err.println("创建工作区目录失败: " + e.getMessage());
        }
    }

    /**
     * 获取根目录 D:/LifeRecorder
     */
    public Path getRootDir() {
        return rootDir;
    }

    /**
     * 获取 today 目录
     */
    public Path getTodayDir() {
        return rootDir.resolve("today").normalize();
    }

    /**
     * 获取 today/passive/notes 目录
     */
    public Path getTodayPassiveNotesDir() {
        return getTodayDir().resolve("passive/notes").normalize();
    }

    /**
     * 获取 today/passive/files 目录
     */
    public Path getTodayPassiveFilesDir() {
        return getTodayDir().resolve("passive/files").normalize();
    }

    /**
     * 获取 today/passive/photos 目录
     */
    public Path getTodayPassivePhotosDir() {
        return getTodayDir().resolve("passive/photos").normalize();
    }

    /**
     * 获取 today/ai_inbox/files 目录
     */
    public Path getTodayAiInboxFilesDir() {
        return getTodayDir().resolve("ai_inbox/files").normalize();
    }

    /**
     * 获取 today/ai_inbox/photos 目录
     */
    public Path getTodayAiInboxPhotosDir() {
        return getTodayDir().resolve("ai_inbox/photos").normalize();
    }

    /**
     * 获取 today/ai_generated/markdown 目录
     */
    public Path getTodayAiGeneratedMarkdownDir() {
        return getTodayDir().resolve("ai_generated/markdown").normalize();
    }

    /**
     * 获取 today/ai_generated/json 目录
     */
    public Path getTodayAiGeneratedJsonDir() {
        return getTodayDir().resolve("ai_generated/json").normalize();
    }

    /**
     * 获取 today/context 目录
     */
    public Path getTodayContextDir() {
        return getTodayDir().resolve("context").normalize();
    }

    /**
     * 获取 today/chat 目录
     */
    public Path getTodayChatDir() {
        return getTodayDir().resolve("chat").normalize();
    }

    /**
     * 获取 yesterday 目录
     */
    public Path getYesterdayDir() {
        return rootDir.resolve("yesterday").normalize();
    }

    /**
     * 获取 summaries 目录
     */
    public Path getSummariesDir() {
        return rootDir.resolve("summaries").normalize();
    }

    /**
     * 获取 history 目录
     */
    public Path getHistoryDir() {
        return rootDir.resolve("history").normalize();
    }

    /**
     * 获取 tasks 目录
     */
    public Path getTasksDir() {
        return rootDir.resolve("tasks").normalize();
    }

    /**
     * 获取 memory 目录
     */
    public Path getMemoryDir() {
        return rootDir.resolve("memory").normalize();
    }

    /**
     * 获取 config 目录
     */
    public Path getConfigDir() {
        return rootDir.resolve("config").normalize();
    }

    /**
     * 验证路径是否安全（必须在 workspace 内）
     */
    public void validatePath(Path path) {
        Path normalized = path.normalize();
        if (!normalized.startsWith(rootDir)) {
            throw new SecurityException("禁止访问 workspace 外的路径: " + path);
        }
    }

    /**
     * 解析相对路径为绝对路径（安全检查）
     */
    public Path resolveRelativePath(String relativePath) {
        Path resolved = getTodayDir().resolve(relativePath).normalize();
        validatePath(resolved);
        return resolved;
    }
}
