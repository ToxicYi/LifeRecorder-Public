package com.jiacun.liferecorderserver.controller;

import com.jiacun.liferecorderserver.dto.AiChatRequest;
import com.jiacun.liferecorderserver.dto.AiChatResponse;
import com.jiacun.liferecorderserver.service.MimoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private MimoClient mimoClient;

    @PostMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<AiChatResponse> testAi(@RequestBody AiChatRequest request) {
        // 检查消息是否为空
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AiChatResponse("请输入内容"));
        }

        // 调用MiMo客户端
        String reply = mimoClient.chatWithMimo(request.getMessage());
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(new AiChatResponse(reply));
    }

    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        // 检查消息是否为空
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AiChatResponse("请输入内容"));
        }

        // 调用MiMo客户端
        String reply = mimoClient.chatWithMimo(request.getMessage());
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(new AiChatResponse(reply));
    }
}
