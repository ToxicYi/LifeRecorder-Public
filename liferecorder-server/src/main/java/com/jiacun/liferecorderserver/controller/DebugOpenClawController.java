package com.jiacun.liferecorderserver.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 临时调试接口 - 用于排查 OpenClaw Gateway 调用
 */
@RestController
@RequestMapping("/debug")
public class DebugOpenClawController {

    private static final String OPENCLAW_CMD = "C:\\Users\\31439\\AppData\\Roaming\\npm\\openclaw.cmd";
    private static final String WORK_DIR = "C:\\Users\\31439";

    /**
     * 测试 Spring 是否能执行 PowerShell
     */
    @GetMapping(value = "/process-test", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> processTest() {
        System.out.println("[ProcessTest] endpoint called");

        try {
            // 构建命令
            List<String> commandList = new ArrayList<>();
            commandList.add("cmd.exe");
            commandList.add("/c");
            commandList.add("echo SPRING_PROCESS_OK");

            System.out.println("[ProcessTest] starting process");
            for (int i = 0; i < commandList.size(); i++) {
                System.out.println("[ProcessTest] command[" + i + "] = " + commandList.get(i));
            }

            // 启动进程
            ProcessBuilder processBuilder = new ProcessBuilder(commandList);
            processBuilder.redirectErrorStream(true);
            processBuilder.directory(new java.io.File(WORK_DIR));

            Process process = processBuilder.start();
            System.out.println("[ProcessTest] process started, pid=" + process.pid());

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[ProcessTest] " + line);
                    output.append(line).append("\n");
                }
            }

            // 等待进程完成
            int exitCode = process.waitFor();
            System.out.println("[ProcessTest] exitCode=" + exitCode);

            return ResponseEntity.ok(output.toString().trim());

        } catch (Exception e) {
            System.err.println("[ProcessTest] error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * 测试 OpenClaw 版本（验证环境变量配置）
     */
    @GetMapping(value = "/openclaw-version", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> openclawVersion() {
        System.out.println("[OpenClawVersion] endpoint called");

        try {
            // 构建命令
            List<String> commandList = new ArrayList<>();
            commandList.add("cmd.exe");
            commandList.add("/c");
            commandList.add(OPENCLAW_CMD);
            commandList.add("--version");

            System.out.println("[OpenClawVersion] starting process");
            for (int i = 0; i < commandList.size(); i++) {
                System.out.println("[OpenClawVersion] command[" + i + "] = " + commandList.get(i));
            }

            // 启动进程
            ProcessBuilder processBuilder = new ProcessBuilder(commandList);
            processBuilder.redirectErrorStream(true);
            processBuilder.directory(new java.io.File(WORK_DIR));

            // 补充环境变量
            Map<String, String> env = processBuilder.environment();
            String oldPath = env.getOrDefault("Path", env.getOrDefault("PATH", ""));
            env.put("Path",
                "C:\\Program Files\\nodejs;" +
                "C:\\Users\\31439\\AppData\\Roaming\\npm;" +
                oldPath
            );
            env.put("USERPROFILE", "C:\\Users\\31439");
            env.put("APPDATA", "C:\\Users\\31439\\AppData\\Roaming");

            System.out.println("[OpenClawVersion] environment configured");

            Process process = processBuilder.start();
            System.out.println("[OpenClawVersion] process started, pid=" + process.pid());

            // 读取输出（带超时）
            StringBuilder output = new StringBuilder();
            Thread readThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[OpenClaw] " + line);
                        output.append(line).append("\n");
                    }
                } catch (Exception e) {
                    System.err.println("读取输出失败: " + e.getMessage());
                }
            });
            readThread.start();

            // 等待进程完成（30秒超时）
            boolean completed = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                System.out.println("[OpenClawVersion] timeout after 30 seconds");
                return ResponseEntity.status(500).body("Error: OpenClaw version check timeout");
            }

            readThread.join(5000);
            int exitCode = process.exitValue();
            System.out.println("[OpenClawVersion] exitCode=" + exitCode);

            return ResponseEntity.ok(output.toString().trim());

        } catch (Exception e) {
            System.err.println("[OpenClawVersion] error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * 调试 OpenClaw Gateway 调用（直接使用 ProcessBuilder）
     */
    @PostMapping(value = "/openclaw", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<Map<String, Object>> debugOpenClaw() {
        System.out.println("[DebugOpenClaw] endpoint called");

        Map<String, Object> response = new HashMap<>();

        try {
            // 构建命令参数列表
            List<String> commandList = new ArrayList<>();
            commandList.add("cmd.exe");
            commandList.add("/c");
            commandList.add(OPENCLAW_CMD);
            commandList.add("agent");
            commandList.add("--session-id");
            commandList.add("liferecorder-debug-openclaw");
            commandList.add("--thinking");
            commandList.add("off");
            commandList.add("--json");
            commandList.add("--timeout");
            commandList.add("180");
            commandList.add("--message");
            commandList.add("只回复 OpenClaw OK，不要读取文件。");

            System.out.println("[DebugOpenClaw] starting process");
            for (int i = 0; i < commandList.size(); i++) {
                System.out.println("[DebugOpenClaw] command[" + i + "] = " + commandList.get(i));
            }

            // 启动进程
            ProcessBuilder processBuilder = new ProcessBuilder(commandList);
            processBuilder.redirectErrorStream(true); // 合并 stderr 到 stdout

            // 设置工作目录
            processBuilder.directory(new java.io.File(WORK_DIR));

            // 补充环境变量
            Map<String, String> env = processBuilder.environment();
            String oldPath = env.getOrDefault("Path", env.getOrDefault("PATH", ""));
            env.put("Path",
                "C:\\Program Files\\nodejs;" +
                "C:\\Users\\31439\\AppData\\Roaming\\npm;" +
                oldPath
            );
            env.put("USERPROFILE", "C:\\Users\\31439");
            env.put("APPDATA", "C:\\Users\\31439\\AppData\\Roaming");

            System.out.println("[DebugOpenClaw] environment configured");

            Process process = processBuilder.start();
            System.out.println("[DebugOpenClaw] process started, pid=" + process.pid());

            // 读取输出（带超时）
            StringBuilder output = new StringBuilder();
            Thread readThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[OpenClaw] " + line);
                        output.append(line).append("\n");
                    }
                } catch (Exception e) {
                    System.err.println("读取输出失败: " + e.getMessage());
                }
            });
            readThread.start();

            // 等待进程完成（180秒超时）
            boolean completed = process.waitFor(180, java.util.concurrent.TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                System.out.println("[DebugOpenClaw] timeout after 180 seconds");
                response.put("success", false);
                response.put("error", "Agent execution timeout after 180 seconds");
                response.put("message", "Agent 执行超时");
                return ResponseEntity.status(500).body(response);
            }

            readThread.join(5000);
            int exitCode = process.exitValue();
            System.out.println("[DebugOpenClaw] exitCode=" + exitCode);

            // 返回原始输出
            response.put("success", true);
            response.put("output", output.toString().trim());
            response.put("exitCode", exitCode);
            response.put("message", "OpenClaw 调用成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("[DebugOpenClaw] error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "OpenClaw 调用失败");
            return ResponseEntity.status(500).body(response);
        }
    }
}
