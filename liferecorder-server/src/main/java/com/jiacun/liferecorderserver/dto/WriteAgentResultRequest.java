package com.jiacun.liferecorderserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Agent 工具受控写入接口 - 请求体
 * 
 * POST /agent-tools/write-result
 * 
 * 安全策略：
 * 1. 第一版只允许 localhost 调用（通过 remoteAddr 检查）
 * 2. 不允许删除文件
 * 3. 不允许执行命令
 * 4. 不允许写入 D:\LifeRecorder 之外
 */
public class WriteAgentResultRequest {
    
    // 文件类型：markdown 或 json
    private String type;
    
    // 文件名，例如 android_test_file_summary.md
    private String name;
    
    // 文件内容
    private String content;
    
    // 来源标识，默认 openclaw_agent
    private String source;

    public WriteAgentResultRequest() {
    }

    public WriteAgentResultRequest(String type, String name, String content, String source) {
        this.type = type;
        this.name = name;
        this.content = content;
        this.source = source;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}