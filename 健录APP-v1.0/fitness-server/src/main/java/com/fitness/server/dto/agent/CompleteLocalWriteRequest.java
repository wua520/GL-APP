package com.fitness.server.dto.agent;

/**
 * 完成本地写入请求
 * 
 * 两阶段提交的第二阶段：客户端成功写入本地后调用
 */
public class CompleteLocalWriteRequest {
    
    private Long actionId;
    private String localReference;  // 本地保存的记录ID（如训练计划的本地ID）
    
    public CompleteLocalWriteRequest() {
    }
    
    public CompleteLocalWriteRequest(Long actionId, String localReference) {
        this.actionId = actionId;
        this.localReference = localReference;
    }
    
    public Long getActionId() {
        return actionId;
    }
    
    public void setActionId(Long actionId) {
        this.actionId = actionId;
    }
    
    public String getLocalReference() {
        return localReference;
    }
    
    public void setLocalReference(String localReference) {
        this.localReference = localReference;
    }
}
