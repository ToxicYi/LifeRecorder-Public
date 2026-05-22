package com.jiacun.liferecorderserver.dto;

public class AiRouteDecision {
    private String route; // "ai" 或 "agent"
    private String tool; // "call_agent" 或 null
    private String taskType; // chat, daily_summary, image_to_markdown, etc.
    private String userIntent;
    private boolean needAttachments;
    private String expectedOutput; // text_reply, markdown_file, json_file, search_result
    private String urgency; // sync 或 async
    private String reason;

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public String getTool() { return tool; }
    public void setTool(String tool) { this.tool = tool; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getUserIntent() { return userIntent; }
    public void setUserIntent(String userIntent) { this.userIntent = userIntent; }
    public boolean isNeedAttachments() { return needAttachments; }
    public void setNeedAttachments(boolean needAttachments) { this.needAttachments = needAttachments; }
    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
