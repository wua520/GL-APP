package com.fitness.server.dto;

public class AiChatResponse {
    private String message;
    
    public AiChatResponse() {}
    
    public AiChatResponse(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
