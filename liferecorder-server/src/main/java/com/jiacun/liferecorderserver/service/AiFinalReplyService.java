package com.jiacun.liferecorderserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI 最终回复整理服务 - 把 Agent 的原始结果整理成用户友好的回复
 */
@Service
public class AiFinalReplyService {

    @Value("${mimo.api-key:}")
    private String apiKey;

    @Value("${mimo.base-url:https://token-plan-cn.xiaomimimo.com/v1}")
    private String baseUrl;

    @Value("${mimo.model:mimo-v2.5}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String REPLY_PROMPT = """
            下面是 Agent 执行结果，请整理成给用户看的简短中文回复。
            
            要求：
            1. 告诉用户任务是否完成
            2. 如果生成了文件，说明文件名和相对路径
            3. 不要暴露完整系统日志
            4. 不要夸张
            5. 不要说自己直接读取了手机文件，应该说"Agent 已处理工作区数据"
            
            Agent 结果：
            %s
            """;

    public AiFinalReplyService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 构建最终回复
     */
    public String buildReply(String agentResult) {
        try {
            String prompt = String.format(REPLY_PROMPT, agentResult);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.add(userMsg);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String apiUrl = baseUrl.trim();
            if (apiUrl.endsWith("/")) {
                apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
            }
            String url = apiUrl + "/chat/completions";

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            if (jsonResponse.has("choices") && jsonResponse.get("choices").isArray() &&
                jsonResponse.get("choices").size() > 0) {

                return jsonResponse.get("choices").get(0)
                    .get("message").get("content").asText();
            }

            // 如果解析失败，返回原始结果
            return agentResult;

        } catch (Exception e) {
            System.err.println("整理 Agent 回复失败: " + e.getMessage());
            // 返回原始结果
            return agentResult;
        }
    }
}
