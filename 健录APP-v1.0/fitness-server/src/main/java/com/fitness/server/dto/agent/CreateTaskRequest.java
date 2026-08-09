package com.fitness.server.dto.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建Agent任务请求
 */
public class CreateTaskRequest {
    
    @NotBlank(message = "任务内容不能为空")
    @Size(max = 2000, message = "任务内容不能超过2000字符")
    private String content;
    
    @Size(max = 100, message = "会话ID不能超过100字符")
    private String sessionId;
    
    // Getters and Setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
