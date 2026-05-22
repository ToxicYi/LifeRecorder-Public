package com.jiacun.liferecorderserver;

import com.jiacun.liferecorderserver.service.LifeChangesService;
import com.jiacun.liferecorderserver.service.LifeIndexService;
import com.jiacun.liferecorderserver.service.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

// 新增：接收 LifeRecorder 手机端上传的数据
@RestController
@RequestMapping("/life")
public class LifeController {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private LifeIndexService lifeIndexService;

    @Autowired
    private LifeChangesService lifeChangesService;

    // 新增：电脑上的长期存档目录
    private static final Path BASE_DIR = Path.of("D:/LifeRecorder/days");

    // 新增：上传当天笔记
    @PostMapping("/upload-note")
    public String uploadNote(@RequestBody Map<String, String> body) {
        try {
            // 新增：读取手机传来的日期、标题、正文
            String date = body.getOrDefault("date", "");
            String title = body.getOrDefault("title", "无标题");
            String content = body.getOrDefault("content", "");

            // 新增：简单校验日期，避免乱写路径
            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return "上传失败：日期格式必须是 yyyy-MM-dd";
            }

            // 新增：创建当天文件夹，例如 D:/LifeRecorder/days/2026-05-11
            Path dayDir = BASE_DIR.resolve(date);
            Files.createDirectories(dayDir);

            // 新增：生成 notes.md 内容
            String markdown = """
                    # %s
                    
                    日期：%s
                    
                    ## 原始笔记
                    
                    %s
                    """.formatted(title, date, content);

            // 新增：保存 Markdown 文件
            Files.writeString(
                    dayDir.resolve("notes.md"),
                    markdown,
                    StandardCharsets.UTF_8
            );

            // 新增：保存 raw.json，方便以后 QClaw / App 重新读取
            String json = """
                    {
                      "date": "%s",
                      "title": "%s",
                      "content": "%s"
                    }
                    """.formatted(
                    escapeJson(date),
                    escapeJson(title),
                    escapeJson(content)
            );

            Files.writeString(
                    dayDir.resolve("raw.json"),
                    json,
                    StandardCharsets.UTF_8
            );

            // ===== 新增：写入 Workspace Protocol v1 新结构 =====
            writeToNewStructure(date, title, content);

            return "上传成功：" + dayDir;

        } catch (Exception e) {
            return "上传失败：" + e.getMessage();
        }
    }

    // 新增：简单处理 JSON 特殊字符
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // 新增：读取某一天的 AI 总结（支持多版本优先级，date 可选）
    @GetMapping("/summary")
    public String getSummary(@RequestParam(required = false) String date) {
        try {
            String targetDate;
            
            // 如果传了 date，使用指定日期；否则查找最新日期
            if (date != null && !date.trim().isEmpty()) {
                // 校验日期格式
                if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return "读取失败：日期格式必须是 yyyy-MM-dd";
                }
                targetDate = date.trim();
            } else {
                // 自动查找最新日期目录
                targetDate = findLatestDate();
                if (targetDate == null) {
                    return "还没有任何日期记录";
                }
            }

            Path dayDir = BASE_DIR.resolve(targetDate);
            
            // 检查日期目录是否存在
            if (!Files.exists(dayDir)) {
                return "日期 " + targetDate + " 的记录不存在";
            }

            // 按优先级查找总结文件
            String[] summaryFiles = {
                "summary_context.md",  // 优先级最高：结合 daily_context.json 的新版总结
                "summary_agent.md",    // 优先级中：Agent 生成的普通总结
                "summary.md"           // 优先级最低：旧版总结
            };

            for (String filename : summaryFiles) {
                Path summaryPath = dayDir.resolve(filename);
                if (Files.exists(summaryPath)) {
                    String content = Files.readString(summaryPath, StandardCharsets.UTF_8);
                    return "当前总结来源：" + filename + "\n\n" + content;
                }
            }

            // 三个文件都不存在
            return targetDate + " 还没有 AI 总结";

        } catch (Exception e) {
            return "读取总结失败：" + e.getMessage();
        }
    }

    /**
     * 查找最新的日期目录
     */
    private String findLatestDate() {
        try {
            if (!Files.exists(BASE_DIR)) {
                return null;
            }

            // 获取所有日期目录，按名称排序（yyyy-MM-dd 格式可以直接字符串比较）
            try (Stream<Path> paths = Files.list(BASE_DIR)) {
                return paths
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.matches("\\d{4}-\\d{2}-\\d{2}"))
                    .sorted(Comparator.reverseOrder())
                    .findFirst()
                    .orElse(null);
            }
        } catch (Exception e) {
            System.err.println("查找最新日期失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 写入 Workspace Protocol v1 新结构
     */
    private void writeToNewStructure(String date, String title, String content) {
        try {
            long now = System.currentTimeMillis();
            
            // 1. 保存笔记到 today/passive/notes/notes.md
            Path notesPath = workspaceService.getTodayPassiveNotesDir().resolve("notes.md");
            String markdown = """
                    # %s
                    
                    日期：%s
                    
                    ## 原始笔记
                    
                    %s
                    """.formatted(title, date, content);
            Files.writeString(notesPath, markdown, StandardCharsets.UTF_8);
            
            // 2. 更新 index.json
            LifeIndexService.IndexItem item = new LifeIndexService.IndexItem();
            item.setId("note_today");
            item.setType("note");
            item.setName("notes.md");
            item.setRelativePath("passive/notes/notes.md");
            item.setMimeType("text/markdown");
            item.setSize(Files.size(notesPath));
            item.setSource("android_app");
            // 提取前100字作为预览
            String preview = content.length() > 100 ? content.substring(0, 100) : content;
            item.setPreview(preview);
            item.setCreatedTime(now);
            item.setUpdatedTime(now);
            lifeIndexService.addOrUpdateItem(item);
            
            // 3. 追加 changes.json
            LifeChangesService.ChangeEntry change = new LifeChangesService.ChangeEntry();
            change.setType("note_updated");
            change.setTargetId("note_today");
            change.setTargetPath("passive/notes/notes.md");
            change.setSource("android_app");
            change.setDescription("用户更新了当天笔记");
            change.setCreatedTime(now);
            lifeChangesService.appendChange(change);
            
        } catch (Exception e) {
            System.err.println("写入新结构失败: " + e.getMessage());
            // 不抛出异常，避免影响原有功能
        }
    }
}