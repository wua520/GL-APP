package com.fitness.server.dto.agent;

import jakarta.validation.constraints.NotNull;

/**
 * 确认操作请求
 */
public class ConfirmActionRequest {
    
    @NotNull(message = "操作ID不能为空")
    private Long actionId;
    
    // Getters and Setters
    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }
}
