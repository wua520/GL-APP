package com.fitness.server.dto.agent;

import java.util.List;

/**
 * Agent任务响应
 */
public class TaskResponse {
    
    private Long taskId;
    private String status;
    private String assistantMessage;
    private List<TaskStep> steps;
    // 兼容旧客户端：始终指向 pendingActions 的首项。
    private PendingAction pendingAction;
    // 一个任务可以同时包含多份彼此独立的待确认草案。
    private List<PendingAction> pendingActions;
    private String failureReason;
    
    // Getters and Setters
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getAssistantMessage() { return assistantMessage; }
    public void setAssistantMessage(String assistantMessage) { this.assistantMessage = assistantMessage; }
    
    public List<TaskStep> getSteps() { return steps; }
    public void setSteps(List<TaskStep> steps) { this.steps = steps; }
    
    public PendingAction getPendingAction() { return pendingAction; }
    public void setPendingAction(PendingAction pendingAction) { this.pendingAction = pendingAction; }

    public List<PendingAction> getPendingActions() { return pendingActions; }
    public void setPendingActions(List<PendingAction> pendingActions) { this.pendingActions = pendingActions; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    /**
     * 任务步骤
     */
    public static class TaskStep {
        private String type;
        private String label;
        private String toolName;
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        
        public String getToolName() { return toolName; }
        public void setToolName(String toolName) { this.toolName = toolName; }
    }
    
    /**
     * 待确认操作
     */
    public static class PendingAction {
        private Long actionId;
        private String type;
        private String status;
        private Long expiresAt;
        private Object preview;
        private String payload;  // 完整的草案JSON
        
        // Getters and Setters
        public Long getActionId() { return actionId; }
        public void setActionId(Long actionId) { this.actionId = actionId; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Long getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }
        
        public Object getPreview() { return preview; }
        public void setPreview(Object preview) { this.preview = preview; }
        
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}
