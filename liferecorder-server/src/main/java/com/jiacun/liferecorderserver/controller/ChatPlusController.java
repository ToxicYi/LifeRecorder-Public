package com.jiacun.liferecorderserver.controller;

import com.jiacun.liferecorderserver.dto.ChatPlusRequest;
import com.jiacun.liferecorderserver.dto.ChatPlusResponse;
import com.jiacun.liferecorderserver.service.ChatPlusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ChatPlus 控制器 - 统一 AI 入口
 * 
 * 提供智能 AI 聊天接口，支持两种模式：
 * 1. 普通 AI 模式 - 直接调用 MiMo API 进行对话
 * 2. Agent 模式 - 调用 OpenClaw Agent 执行复杂任务（读取工作区、生成文件等）
 * 
 * 路由决策由 AiRouterService 自动判断，也可通过 forceAgent 参数强制使用 Agent 模式
 */
@RestController
@RequestMapping("/ai")
public class ChatPlusController {

    /**
     * ChatPlus 服务，负责处理 AI 聊天和 Agent 调用逻辑
     */
    @Autowired
    private ChatPlusService chatPlusService;

    /**
     * 统一 AI 聊天接口
     * 
     * 根据用户消息内容自动判断使用普通 AI 还是 Agent 模式：
     * - 简单对话、问答 → 普通 AI 模式
     * - 需要读取文件、生成总结、分析数据 → Agent 模式
     * 
     * @param request 聊天请求，包含：
     *   - message: 用户消息文本
     *   - forceAgent: 是否强制使用 Agent 模式（用于调试）
     * @return 聊天响应，包含：
     *   - reply: AI 回复文本
     *   - mode: 使用的模式（"ai" 或 "agent"）
     *   - taskType: 任务类型
     *   - generatedPath: 如果 Agent 生成了文件，返回文件相对路径
     */
    @PostMapping(value = "/chat-plus", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<ChatPlusResponse> chatPlus(@RequestBody ChatPlusRequest request) {
        try {
            // 验证消息是否为空
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ChatPlusResponse("请输入内容", "ai", "chat", null));
            }

            // 处理请求（传递 attachments）
            ChatPlusService.ChatPlusResult result = chatPlusService.processChatPlus(
                request.getMessage(),
                request.getForceAgent(),
                request.getAttachments()
            );

            // 构建响应对象
            ChatPlusResponse response = new ChatPlusResponse(
                result.getReply(),
                result.getMode(),
                result.getTaskType(),
                result.getGeneratedPath(),
                result.getGeneratedFiles()
            );

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);

        } catch (Exception e) {
            // 异常处理：返回错误信息
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChatPlusResponse("处理失败：" + e.getMessage(), "ai", "chat", null));
        }
    }
}
