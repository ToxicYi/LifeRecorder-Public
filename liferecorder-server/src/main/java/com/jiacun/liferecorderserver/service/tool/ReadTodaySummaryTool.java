package com.jiacun.liferecorderserver.service.tool;

import com.jiacun.liferecorderserver.service.LifeRecorderWorkspace;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * 读取今日 summary.md 的工具
 */
@Component
public class ReadTodaySummaryTool {

    private final LifeRecorderWorkspace workspace;

    public ReadTodaySummaryTool(LifeRecorderWorkspace workspace) {
        this.workspace = workspace;
    }

    /**
     * 读取指定日期的 summary.md
     * @param date 日期
     * @return summary.md 的内容
     */
    public String execute(LocalDate date) {
        try {
            Path dayDir = workspace.getDayDirectory(date);
            Path summaryPath = dayDir.resolve("summary.md");

            // 安全检查
            workspace.validateFilePath(summaryPath);

            if (!Files.exists(summaryPath)) {
                return "该日期还没有 summary.md 文件";
            }

            String content = Files.readString(summaryPath, StandardCharsets.UTF_8);
            return "今日摘要内容：\n\n" + content;

        } catch (IOException e) {
            return "读取 summary.md 失败：" + e.getMessage();
        } catch (SecurityException e) {
            return "安全错误：" + e.getMessage();
        }
    }
}
