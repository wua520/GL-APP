package com.fitness.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Agent 待确认操作实体
 */
@TableName("agent_actions")
public class AgentAction {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String type;
    private String payloadJson;
    private String status;
    private String idempotencyKey;
    private Long expiresAt;
    private Long executedAt;
    private String resultJson;
    private String localReference;  // 客户端本地引用 JSON（类型与有序本地记录ID）
    private String failureReason;
    private Long createdAt;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    
    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }
    
    public Long getExecutedAt() { return executedAt; }
    public void setExecutedAt(Long executedAt) { this.executedAt = executedAt; }
    
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    
    public String getLocalReference() { return localReference; }
    public void setLocalReference(String localReference) { this.localReference = localReference; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}
