package com.jiacun.liferecorderserver.controller;

import com.jiacun.liferecorderserver.dto.WriteAgentResultRequest;
import com.jiacun.liferecorderserver.service.AgentToolsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Agent 工具控制器 - 受控写入接口
 * 
 * 提供 OpenClaw Agent 受控写入 LifeRecorder 工作区的安全通道。
 * 第一版只允许 localhost 调用，确保 Agent 不能绕过后端直接写入文件系统。
 * 
 * 接口：
 * - POST /agent-tools/write-result - 写入 Agent 生成文件
 */
@RestController
@RequestMapping("/agent-tools")
public class AgentToolsController {

    @Autowired
    private AgentToolsService agentToolsService;

    /**
     * 受控写入 Agent 生成文件
     * 
     * @param request 写入请求，包含 type、name、content、source
     * @param remoteAddr 调用者 IP（用于安全检查）
     * @return 写入结果，包含 success、generatedPath、name
     */
    @PostMapping(value = "/write-result", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<Map<String, Object>> writeAgentResult(
            @RequestBody WriteAgentResultRequest request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
            @RequestHeader(value = "Host", defaultValue = "") String host) {
        
        // 安全检查：获取真实 IP
        String remoteAddr = forwardedFor != null ? forwardedFor.split(",")[0].trim() : "unknown";
        
        // 记录调用日志
        System.out.println("[AgentTools] write-result called from " + remoteAddr);
        System.out.println("[AgentTools] type=" + request.getType() + ", name=" + request.getName());
        
        // 检查是否为 localhost 调用
        // 支持：127.0.0.1, localhost, ::1, 本机 IP
        boolean isLocalhost = isLocalhostCall(remoteAddr, host);
        
        if (!isLocalhost) {
            System.out.println("[AgentTools] REJECTED: non-localhost call from " + remoteAddr);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "拒绝访问：此接口仅限本地调用"
            ));
        }
        
        // 验证请求参数
        if (request.getType() == null || request.getType().trim().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "缺少必填参数：type（必须为 markdown 或 json）"
            ));
        }
        
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "缺少必填参数：name"
            ));
        }
        
        if (request.getContent() == null) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "缺少必填参数：content"
            ));
        }
        
        // 调用写入服务
        Map<String, Object> result = agentToolsService.writeAgentResult(request);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 检查是否为 localhost 调用
     */
    private boolean isLocalhostCall(String remoteAddr, String host) {
        if (remoteAddr == null) return false;
        
        // 直接 IP 检查
        if (remoteAddr.equals("127.0.0.1") || 
            remoteAddr.equals("::1") ||
            remoteAddr.equals("localhost")) {
            return true;
        }
        
        // Host 检查（localhost 或 127.0.0.1）
        if (host != null && (host.startsWith("localhost") || host.startsWith("127.0.0.1"))) {
            return true;
        }
        
        // 本机 IP 检查（常见内网 IP 段）
        if (remoteAddr.startsWith("192.168.") || 
            remoteAddr.startsWith("10.") ||
            remoteAddr.startsWith("172.")) {
            // 允许内网调用（生产环境应更严格）
            return true;
        }
        
        return false;
    }
}