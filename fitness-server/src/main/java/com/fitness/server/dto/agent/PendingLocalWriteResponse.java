package com.fitness.server.dto.agent;

/**
 * 待完成本地写入的操作响应
 */
public class PendingLocalWriteResponse {
    
    private Long taskId;
    private Long actionId;
    private String type;
    private String payloadJson;
    private Long createdAt;
    
    public PendingLocalWriteResponse() {
    }
    
    public PendingLocalWriteResponse(Long taskId, Long actionId, String type, String payloadJson, Long createdAt) {
        this.taskId = taskId;
        this.actionId = actionId;
        this.type = type;
        this.payloadJson = payloadJson;
        this.createdAt = createdAt;
    }
    
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    
    public Long getActionId() {
        return actionId;
    }
    
    public void setActionId(Long actionId) {
        this.actionId = actionId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getPayloadJson() {
        return payloadJson;
    }
    
    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
