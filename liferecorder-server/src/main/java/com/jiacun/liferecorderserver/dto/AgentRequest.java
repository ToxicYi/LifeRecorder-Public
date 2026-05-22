package com.jiacun.liferecorderserver.dto;

public class AgentRequest {
    private String message;
    private String date; // 可选，默认为今天

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
