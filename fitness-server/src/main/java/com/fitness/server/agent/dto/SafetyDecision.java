package com.fitness.server.agent.dto;

import java.util.List;

/**
 * Safety Policy的安全决策
 * F1-A: 不可绕过的安全约束
 */
public class SafetyDecision {
    
    private RiskLevel riskLevel;
    private List<String> restrictions;  // 限制列表
    private List<String> forbiddenActions;  // 禁止的动作
    private String responseGuidance;  // 回复指导
    private boolean blockDraft;  // 是否阻断草案生成
    private String escalationAdvice;  // 升级建议（可选）
    
    public enum RiskLevel {
        SAFE,       // 安全
        CAUTION,    // 需要注意
        HIGH,       // 高风险
        BLOCK       // 必须阻断
    }

    // Getters and Setters
    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(List<String> restrictions) {
        this.restrictions = restrictions;
    }

    public List<String> getForbiddenActions() {
        return forbiddenActions;
    }

    public void setForbiddenActions(List<String> forbiddenActions) {
        this.forbiddenActions = forbiddenActions;
    }

    public String getResponseGuidance() {
        return responseGuidance;
    }

    public void setResponseGuidance(String responseGuidance) {
        this.responseGuidance = responseGuidance;
    }

    public boolean isBlockDraft() {
        return blockDraft;
    }

    public void setBlockDraft(boolean blockDraft) {
        this.blockDraft = blockDraft;
    }

    public String getEscalationAdvice() {
        return escalationAdvice;
    }

    public void setEscalationAdvice(String escalationAdvice) {
        this.escalationAdvice = escalationAdvice;
    }

    /**
     * 安全决策必须有明确风险级别；阻断级别永远禁止草案。
     */
    public void validate() {
        if (riskLevel == null || restrictions == null || forbiddenActions == null) {
            throw new IllegalArgumentException("安全决策契约不完整");
        }
        if (riskLevel == RiskLevel.BLOCK && !blockDraft) {
            throw new IllegalArgumentException("阻断级别不能允许草案");
        }
    }
}
