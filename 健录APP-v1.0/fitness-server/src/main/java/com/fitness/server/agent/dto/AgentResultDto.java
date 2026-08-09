package com.fitness.server.agent.dto;

import java.util.List;
import java.util.Map;

/**
 * 子Agent返回给Supervisor的结构化结果
 * F1-A: 可追溯的事实和结论
 */
public class AgentResultDto {
    
    private String taskId;
    private String agentName;
    private ResultStatus status;
    private List<Map<String, Object>> facts;  // 工具结果提炼的可追溯事实
    private List<String> findings;  // 发现和分析
    private List<String> constraints;  // 约束和限制
    private double confidence;  // 0-1
    private List<String> evidenceRefs;  // 证据引用
    private String draftCandidate;  // 草案候选JSON字符串（可选）
    private String failureReason;  // 失败原因（可选）
    
    public enum ResultStatus {
        SUCCESS,   // 完全成功
        PARTIAL,   // 部分成功
        BLOCKED,   // 被安全策略阻断
        FAILED     // 执行失败
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public ResultStatus getStatus() {
        return status;
    }

    public void setStatus(ResultStatus status) {
        this.status = status;
    }

    public List<Map<String, Object>> getFacts() {
        return facts;
    }

    public void setFacts(List<Map<String, Object>> facts) {
        this.facts = facts;
    }

    public List<String> getFindings() {
        return findings;
    }

    public void setFindings(List<String> findings) {
        this.findings = findings;
    }

    public List<String> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<String> constraints) {
        this.constraints = constraints;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public List<String> getEvidenceRefs() {
        return evidenceRefs;
    }

    public void setEvidenceRefs(List<String> evidenceRefs) {
        this.evidenceRefs = evidenceRefs;
    }

    public String getDraftCandidate() {
        return draftCandidate;
    }

    public void setDraftCandidate(String draftCandidate) {
        this.draftCandidate = draftCandidate;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    /**
     * 结果是跨边界数据，聚合前拒绝缺失身份、非法置信度和空状态。
     */
    public void validateForTrainingTask() {
        if (taskId == null || taskId.isBlank() || !"TrainingAgent".equals(agentName)) {
            throw new IllegalArgumentException("训练结果身份无效");
        }
        if (status == null || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("训练结果状态或置信度无效");
        }
        if (facts == null || findings == null || constraints == null || evidenceRefs == null) {
            throw new IllegalArgumentException("训练结果缺少结构化字段");
        }
        if (draftCandidate != null && draftCandidate.isBlank()) {
            throw new IllegalArgumentException("草案候选不能为空字符串");
        }
    }
}
