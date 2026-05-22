package com.jiacun.liferecorderserver.service;

import com.jiacun.liferecorderserver.dto.AiRouteDecision;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Agent 桥接服务 - 调用 OpenClaw Agent
 */
@Service
public class AgentBridgeService {

    private static final String WORKSPACE_ROOT = "D:/LifeRecorder";
    private static final long AGENT_TIMEOUT_SECONDS = 180;
    private static final String OPENCLAW_CMD = "C:\\Users\\31439\\AppData\\Roaming\\npm\\openclaw.cmd";
    private static final String WORK_DIR = "C:\\Users\\31439";

    private static final String AGENT_PROMPT_TEMPLATE = """
            你现在是 LifeRecorder 后台 Agent，不是普通聊天助手。

            你正在被 Spring Boot 后端调用，用来执行 LifeRecorder 工作区任务。

            LifeRecorder 系统由三部分组成：
            1. Android App：负责选择文件、上传索引、响应 pending request、上传真实文件。
            2. Spring Boot：负责接收数据、维护 D:\\LifeRecorder 工作区、提供 phone_sync 接口。
            3. OpenClaw Agent：负责读取 D:\\LifeRecorder 工作区、理解索引、生成总结、按需请求手机文件。

            你的工作区根目录是：
            D:\\LifeRecorder

            所有 LifeRecorder 相对路径都必须解释为：
            D:\\LifeRecorder\\{relativePath}

            不要把 LifeRecorder 相对路径理解为 OpenClaw 自己的 workspace 路径。

            当你处理手机文件时，必须遵守 phone_sync 工作流：

            1. 优先读取：
            D:\\LifeRecorder\\phone_sync\\current\\app_files_index.json

            2. 如果文件 availableLocally=true 且 cachedPath 不为空：
            读取：
            D:\\LifeRecorder\\{cachedPath}

            3. 如果文件 availableLocally=false 或 cachedPath=null：
            不要编造文件内容。
            不要让用户"手动导出到电脑"。
            你应该说明：该文件真实内容尚未缓存，需要通过 phone_sync pending request 让 Android App 上传。

            4. 如果任务需要继续读取这个文件，应创建或建议创建 fetch_phone_file 请求：
            D:\\LifeRecorder\\phone_sync\\current\\pending_requests.json
            或通过 Spring Boot 的 create-fetch-request 流程创建。

            5. 你应该告诉用户：
            请在手机端 LifeRecorder App 点击"检查 Agent 文件请求"，App 会上传被请求的真实文件。

            6. 你不是普通聊天助手，不要说"需要我帮你设置同步吗？"
            你应该直接按 LifeRecorder 工作流执行或返回当前任务状态。

            读取优先级：
            1. D:\\LifeRecorder\\today\\index.json
            2. D:\\LifeRecorder\\today\\changes.json
            3. D:\\LifeRecorder\\today\\context\\daily_context.json
            4. D:\\LifeRecorder\\today\\chat\\chat_history.json
            5. D:\\LifeRecorder\\phone_sync\\current\\app_files_index.json
            6. D:\\LifeRecorder\\phone_sync\\current\\virtual_folders.json
            7. D:\\LifeRecorder\\phone_sync\\current\\file_index.json
            8. D:\\LifeRecorder\\phone_sync\\current\\photo_index.json

            安全边界：
            - 不访问 D:\\LifeRecorder 之外的路径
            - 不删除用户原始文件
            - 不一次性读取所有大文件
            - 不一次性请求上传所有手机文件
            - 不把 virtualPath 当作真实手机路径
            - 不绕过 Spring Boot 和 Android App 机制访问手机文件

            ---

            【重要】文件生成规范：

            当用户任务包含“生成文件 / 生成 Markdown / 保存总结 / 写入文件 / 生成报告”时，你必须严格遵守以下规则：

            1. 禁止使用 OpenClaw write 工具直接把结果写到 OpenClaw workspace 根目录。
            2. 必须使用 liferecorder-workspace Skill 的脚本：
               C:/Users/31439/.openclaw/workspace/skills/liferecorder-workspace/scripts/write_agent_result.py

            3. 生成 Markdown 文件时必须调用：
               python "C:/Users/31439/.openclaw/workspace/skills/liferecorder-workspace/scripts/write_agent_result.py" --type markdown --name "<安全文件名>.md" --content "<Markdown内容>" --source openclaw_agent

            4. 生成 JSON 文件时必须调用：
               python "C:/Users/31439/.openclaw/workspace/skills/liferecorder-workspace/scripts/write_agent_result.py" --type json --name "<安全文件名>.json" --content "<JSON内容>" --source openclaw_agent

            5. 该脚本会把结果自动写入：
               - Markdown: D:/LifeRecorder/today/ai_generated/markdown
               - JSON: D:/LifeRecorder/today/ai_generated/json

            6. Agent 最终回复必须包含脚本返回的 relativePath，例如：
               ai_generated/markdown/xxx.md
               或
               ai_generated/json/xxx.json

            7. 文件名必须是安全的（只允许字母、数字、下划线、中划线），不要包含特殊字符。

            8. 如果任务不需要生成文件，则不需要调用此脚本。

            ---

            忽略 BOOTSTRAP 初始化流程。
            不要询问名字、身份、风格、emoji。
            不要更新 IDENTITY.md、USER.md，也不要删除 BOOTSTRAP.md。
            当前任务优先级高于初始化引导。
            只执行本次 LifeRecorder 任务。
            
            请严格按照：
            %s/config/workspace_protocol_v1.md
            
            用户意图：
            %s
            
            任务类型：
            %s
            
            请读取以下文件（如果文件不存在则跳过）：
            - %s/today/index.json（必须读取）
            - %s/today/changes.json（必须读取）
            - %s/today/context/daily_context.json（可选，如果不存在则跳过）
            - %s/today/chat/chat_history.json（可选，如果不存在则跳过）
            
            如果需要附件，请按 index.json 中的 relativePath 从 today/ai_inbox 读取。
            
            如果需要生成文件，请保存到：
            %s/today/ai_generated/markdown
            或：
            %s/today/ai_generated/json
            
            禁止：
            - 删除文件
            - 移动文件
            - 覆盖原始文件
            - 访问 %s 之外路径
            - 执行危险命令
            
            完成后请返回：
            1. 是否成功
            2. 生成了什么文件
            3. 相对路径
            4. 简短结果摘要
            """;

    /**
     * 调用 OpenClaw Agent
     */
    public String callAgent(AiRouteDecision decision) {
        return callAgent(decision, false); // 默认不合并错误流
    }

    /**
     * 调用 OpenClaw Agent（支持合并错误流）
     * @param decision 路由决策
     * @param mergeErrorStream 是否合并 stderr 到 stdout（调试模式用）
     */
    public String callAgent(AiRouteDecision decision, boolean mergeErrorStream) {
        File promptFile = null;
        try {
            // 1. 检查 OpenClaw.cmd 是否存在
            File openclawCmd = new File(OPENCLAW_CMD);
            if (!openclawCmd.exists()) {
                return "OpenClaw.cmd 不存在：" + OPENCLAW_CMD;
            }

            // 3. 构建 LifeRecorder runtime prompt（所有 Agent 模式都必须注入）
            String agentPrompt = String.format(
                AGENT_PROMPT_TEMPLATE,
                WORKSPACE_ROOT,
                decision.getUserIntent(),
                decision.getTaskType(),
                WORKSPACE_ROOT,
                WORKSPACE_ROOT,
                WORKSPACE_ROOT,
                WORKSPACE_ROOT,
                WORKSPACE_ROOT,
                WORKSPACE_ROOT,
                WORKSPACE_ROOT
            );

            // 4. 生成会话 ID
            long timestamp = System.currentTimeMillis();
            String sessionId = "liferecorder-chatplus-" + timestamp;

            // 5. 构建 shortMessage（所有任务都写 prompt 文件，debug_agent 也要注入 runtime prompt）
            String shortMessage;
            
            // 所有 Agent 模式都必须创建 prompt 文件并注入 LifeRecorder runtime prompt
            String promptFilename = "runtime_prompt_" + sessionId + ".txt";
            promptFile = new File(WORKSPACE_ROOT + "/tasks/" + promptFilename);
            
            // 确保 tasks 目录存在
            promptFile.getParentFile().mkdirs();
            
            try (FileWriter writer = new FileWriter(promptFile, StandardCharsets.UTF_8)) {
                writer.write(agentPrompt);
            }
            
            System.out.println("[Agent] Prompt file created: " + promptFile.getAbsolutePath());
            System.out.println("[AgentPrompt] LifeRecorder runtime prefix injected");
            System.out.println("[AgentPrompt] taskType=" + decision.getTaskType());
            System.out.println("[AgentPrompt] forceAgent=" + decision.getUserIntent()); // 实际是 userIntent，不是 forceAgent，但日志价值在于看到意图
            
            // 打印 promptFile 路径
            String promptFilePath = promptFile.getAbsolutePath().replace("/", "\\");
            System.out.println("[AgentPrompt] promptFile=" + promptFilePath);
            
            shortMessage = "请读取 " + promptFilePath + "，并严格按照其中的 LifeRecorder Agent 任务要求执行。完成后返回执行结果。";

            // 7. 构建 CMD 命令参数列表（Gateway 模式）
            List<String> commandList = new ArrayList<>();
            commandList.add("cmd.exe");
            commandList.add("/c");
            commandList.add(OPENCLAW_CMD);
            commandList.add("agent");
            // Gateway 模式：去掉 --local 和 --model
            commandList.add("--session-id");
            commandList.add(sessionId);
            commandList.add("--thinking");
            commandList.add("off");
            commandList.add("--json");
            commandList.add("--timeout");
            commandList.add("180");
            commandList.add("--message");
            commandList.add(shortMessage);

            // 打印完整命令参数数组
            System.out.println("[Agent] Starting OpenClaw Agent...");
            for (int i = 0; i < commandList.size(); i++) {
                System.out.println("[Agent] command[" + i + "] = " + commandList.get(i));
            }

            // 8. 启动进程
            ProcessBuilder processBuilder = new ProcessBuilder(commandList);
            processBuilder.redirectErrorStream(mergeErrorStream); // 调试模式时合并错误流

            // 设置工作目录
            processBuilder.directory(new File(WORK_DIR));

            // 补充环境变量
            java.util.Map<String, String> env = processBuilder.environment();
            String oldPath = env.getOrDefault("Path", env.getOrDefault("PATH", ""));
            env.put("Path",
                "C:\\Program Files\\nodejs;" +
                "C:\\Users\\31439\\AppData\\Roaming\\npm;" +
                oldPath
            );
            env.put("USERPROFILE", "C:\\Users\\31439");
            env.put("APPDATA", "C:\\Users\\31439\\AppData\\Roaming");

            System.out.println("[Agent] environment configured");
            
            Process process = processBuilder.start();
            
            // 打印进程 PID
            System.out.println("[Agent] Agent process started, pid=" + process.pid());

            // 9. 异步读取输出流（实时打印到控制台）
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            boolean[] hasOutput = {false};

            if (mergeErrorStream) {
                // 调试模式：合并流，统一打印 [OpenClaw]
                Thread outputThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            hasOutput[0] = true;
                            System.out.println("[OpenClaw] " + line);
                            stdout.append(line).append("\n");
                        }
                    } catch (Exception e) {
                        System.err.println("读取输出失败: " + e.getMessage());
                    }
                });
                outputThread.start();
            } else {
                // 正常模式：分开读取 stdout 和 stderr
                Thread stdoutThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            hasOutput[0] = true;
                            System.out.println("[OpenClaw-OUT] " + line);
                            stdout.append(line).append("\n");
                        }
                    } catch (Exception e) {
                        System.err.println("读取 stdout 失败: " + e.getMessage());
                    }
                });

                Thread stderrThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            hasOutput[0] = true;
                            System.err.println("[OpenClaw-ERR] " + line);
                            stderr.append(line).append("\n");
                        }
                    } catch (Exception e) {
                        System.err.println("读取 stderr 失败: " + e.getMessage());
                    }
                });

                stdoutThread.start();
                stderrThread.start();
            }

            // 10. 监控 10 秒内是否有输出
            Thread monitorThread = new Thread(() -> {
                try {
                    TimeUnit.SECONDS.sleep(10);
                    if (!hasOutput[0]) {
                        System.out.println("[Agent] OpenClaw 已启动，但 10 秒内没有输出，可能正在初始化或等待模型响应。");
                    }
                } catch (InterruptedException e) {
                    // 线程被中断，忽略
                }
            });
            monitorThread.setDaemon(true);
            monitorThread.start();

            // 11. 等待进程完成，设置超时
            boolean completed = process.waitFor(AGENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!completed) {
                System.out.println("[Agent] Agent process timeout after " + AGENT_TIMEOUT_SECONDS + " seconds");
                process.destroyForcibly();
                return "Agent 执行超时，任务未完成。";
            }

            int exitCode = process.exitValue();
            System.out.println("[Agent] Agent process exited, exitCode=" + exitCode);
            
            // 12. 处理执行结果
            if (exitCode != 0) {
                String errorMsg = "Agent 执行失败，退出码：" + exitCode;
                if (stderr.length() > 0) {
                    errorMsg += "\n错误信息：\n" + stderr.toString();
                }
                if (stdout.length() > 0) {
                    errorMsg += "\n输出信息：\n" + stdout.toString();
                }
                return errorMsg;
            }

            // 13. 返回成功结果
            String result = stdout.toString();
            if (result.trim().isEmpty() && stderr.length() > 0) {
                // 如果 stdout 为空但 stderr 有内容，返回 stderr
                return stderr.toString();
            }
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("调用 Agent 被中断: " + e.getMessage());
            return "Agent 调用被中断：" + e.getMessage();
        } catch (Exception e) {
            System.err.println("调用 Agent 失败: " + e.getMessage());
            return "Agent 调用失败：" + e.getMessage();
        }
        // 注意：不再在 finally 中删除 prompt 文件，因为 Gateway 模式可能异步读取
    }
}