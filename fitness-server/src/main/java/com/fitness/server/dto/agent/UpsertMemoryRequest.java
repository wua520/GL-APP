package com.fitness.server.dto.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpsertMemoryRequest {
    @NotBlank(message = "记忆内容不能为空")
    @Size(max = 1000, message = "记忆内容不能超过1000字符")
    private String content;

    @NotBlank(message = "记忆类型不能为空")
    @Pattern(regexp = "PROFILE|SAFETY|PLAN|SUMMARY", message = "记忆类型必须是 PROFILE、SAFETY、PLAN 或 SUMMARY")
    private String memoryType;

    @Size(max = 100, message = "会话ID不能超过100字符")
    private String sessionId;
    private Long expiresAt;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMemoryType() { return memoryType; }
    public void setMemoryType(String memoryType) { this.memoryType = memoryType; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }
}
