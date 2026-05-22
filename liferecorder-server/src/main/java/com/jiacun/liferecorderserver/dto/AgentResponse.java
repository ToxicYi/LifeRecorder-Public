package com.jiacun.liferecorderserver.dto;

public class AgentResponse {
    private String reply;
    private boolean success;

    public AgentResponse() {
    }

    public AgentResponse(String reply) {
        this.reply = reply;
        this.success = true;
    }

    public AgentResponse(String reply, boolean success) {
        this.reply = reply;
        this.success = success;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
