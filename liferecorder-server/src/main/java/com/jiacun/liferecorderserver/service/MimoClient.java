package com.jiacun.liferecorderserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MimoClient {

    @Value("${mimo.api-key:}")
    private String apiKey;

    @Value("${mimo.base-url:https://api.mimo.com/v1}")
    private String baseUrl;

    @Value("${mimo.model:mimo-chat}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MimoClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String chatWithMimo(String userMessage) {
        // 检查配置是否完整
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "MiMo 配置缺失，请检查 api-key/base-url/model";
        }
    
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "MiMo 配置缺失，请检查 api-key/base-url/model";
        }
    
        if (model == null || model.trim().isEmpty()) {
            return "MiMo 配置缺失，请检查 api-key/base-url/model";
        }
    
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
    
            // 构建消息列表 - 只保留用户消息，最小化参数
            List<Map<String, String>> messages = new ArrayList<>();
                 
            // 用户消息
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
    
            // 构建请求体 - 最小OpenAI Chat Completions格式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
    
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
    
            // 确保URL正确拼接，避免双斜杠
            String apiUrl = baseUrl.trim();
            if (apiUrl.endsWith("/")) {
                apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
            }
            String url = apiUrl + "/chat/completions";
                
            System.out.println("调用MiMo API: " + url);
            System.out.println("使用模型: " + model);
            System.out.println("请求体: " + objectMapper.writeValueAsString(requestBody));
    
            // 发送请求到MiMo API
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );
    
            // 解析响应
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
            // 检查是否有错误
            if (jsonResponse.has("error")) {
                String errorMessage = jsonResponse.get("error").get("message").asText();
                return "MiMo API 调用失败：" + errorMessage;
            }
    
            // 提取回复内容
            if (jsonResponse.has("choices") && jsonResponse.get("choices").isArray() && 
                jsonResponse.get("choices").size() > 0) {
                JsonNode firstChoice = jsonResponse.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    return firstChoice.get("message").get("content").asText();
                }
            }
    
            return "MiMo API 返回格式异常";
    
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 处理4xx错误，特别是400 Bad Request
            String responseBody = e.getResponseBodyAsString();
            String errorMsg = "MiMo API 调用失败(HTTP " + e.getStatusCode() + ")：";
                
            try {
                // 尝试解析错误响应为JSON
                JsonNode errorJson = objectMapper.readTree(responseBody);
                if (errorJson.has("error") && errorJson.get("error").has("message")) {
                    errorMsg += errorJson.get("error").get("message").asText();
                } else {
                    errorMsg += responseBody;
                }
            } catch (Exception parseEx) {
                // 如果解析失败，直接返回原始响应
                errorMsg += responseBody;
            }
                
            System.err.println(errorMsg);
            return errorMsg;
                
        } catch (Exception e) {
            // 不打印完整的API密钥，只记录错误类型
            String errorMsg = e.getClass().getSimpleName();
            if (e.getMessage() != null) {
                String safeMessage = e.getMessage().replaceAll(apiKey, "***MASKED***");
                errorMsg += ": " + safeMessage.substring(0, Math.min(safeMessage.length(), 100));
            }
            System.err.println("MiMo API调用出错: " + errorMsg);
            return "MiMo API 调用失败：" + e.getMessage();
        }
    }
}
