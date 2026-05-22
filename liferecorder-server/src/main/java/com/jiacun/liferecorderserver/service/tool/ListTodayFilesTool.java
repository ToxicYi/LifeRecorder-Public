package com.jiacun.liferecorderserver.service.tool;

import com.jiacun.liferecorderserver.service.LifeRecorderWorkspace;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 列出今日文件的工具
 */
@Component
public class ListTodayFilesTool {

    private final LifeRecorderWorkspace workspace;

    public ListTodayFilesTool(LifeRecorderWorkspace workspace) {
        this.workspace = workspace;
    }

    /**
     * 列出指定日期的所有文件
     * @param date 日期
     * @return 文件列表（相对路径）
     */
    public String execute(LocalDate date) {
        try {
            Path dayDir = workspace.getDayDirectory(date);
            
            if (!Files.exists(dayDir)) {
                return "该日期目录下暂无文件";
            }

            List<String> fileList;
            try (Stream<Path> paths = Files.walk(dayDir)) {
                fileList = paths
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        // 返回相对于 dayDir 的路径
                        return dayDir.relativize(path).toString();
                    })
                    .collect(Collectors.toList());
            }

            if (fileList.isEmpty()) {
                return "该日期目录下暂无文件";
            }

            return "今日文件列表：\n" + String.join("\n", fileList);

        } catch (IOException e) {
            return "读取文件列表失败：" + e.getMessage();
        } catch (SecurityException e) {
            return "安全错误：" + e.getMessage();
        }
    }
}
