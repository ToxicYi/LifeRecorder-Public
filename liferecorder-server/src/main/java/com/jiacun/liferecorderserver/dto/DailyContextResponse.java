package com.jiacun.liferecorderserver.dto;

public class DailyContextResponse {
    private boolean success;
    private String message;
    private String filePath;

    public DailyContextResponse() {
    }

    public DailyContextResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public DailyContextResponse(boolean success, String message, String filePath) {
        this.success = success;
        this.message = message;
        this.filePath = filePath;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
