package com.jiacun.liferecorderserver.controller;

import com.jiacun.liferecorderserver.dto.AgentRequest;
import com.jiacun.liferecorderserver.dto.AgentResponse;
import com.jiacun.liferecorderserver.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @PostMapping(value = "/agent", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<AgentResponse> agent(@RequestBody AgentRequest request) {
        // 检查消息是否为空
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AgentResponse("请输入内容", false));
        }

        // 调用 Agent 服务
        String reply = agentService.processAgentRequest(
            request.getMessage(), 
            request.getDate()
        );

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(new AgentResponse(reply, true));
    }
}
