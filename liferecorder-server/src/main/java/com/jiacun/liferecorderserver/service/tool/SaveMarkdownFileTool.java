package com.jiacun.liferecorderserver.service.tool;

import com.jiacun.liferecorderserver.service.LifeRecorderWorkspace;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * 保存 Markdown 文件的工具
 */
@Component
public class SaveMarkdownFileTool {

    private final LifeRecorderWorkspace workspace;

    public SaveMarkdownFileTool(LifeRecorderWorkspace workspace) {
        this.workspace = workspace;
    }

    /**
     * 保存 Markdown 文件到 ai_generated 目录
     * @param date 日期
     * @param filename 文件名（不含路径）
     * @param content 文件内容
     * @return 保存结果
     */
    public String execute(LocalDate date, String filename, String content) {
        try {
            // 安全检查：文件名不能包含路径分隔符
            if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
                return "安全错误：非法的文件名";
            }

            // 确保文件名以 .md 结尾
            if (!filename.toLowerCase().endsWith(".md")) {
                filename = filename + ".md";
            }

            Path aiGenDir = workspace.getAiGeneratedDirectory(date);
            
            // 创建目录（如果不存在）
            Files.createDirectories(aiGenDir);

            Path filePath = aiGenDir.resolve(filename).normalize();

            // 安全检查：确保路径在 workspace 内
            workspace.validateFilePath(filePath);

            // 禁止覆盖已有文件
            if (Files.exists(filePath)) {
                return "文件已存在，禁止覆盖：" + filename;
            }

            // 写入文件
            Files.writeString(filePath, content, StandardCharsets.UTF_8);

            return "文件保存成功：" + filePath.toString();

        } catch (IOException e) {
            return "保存文件失败：" + e.getMessage();
        } catch (SecurityException e) {
            return "安全错误：" + e.getMessage();
        }
    }
}
