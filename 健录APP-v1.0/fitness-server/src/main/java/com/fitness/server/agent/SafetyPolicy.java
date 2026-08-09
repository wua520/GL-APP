package com.fitness.server.agent;

import com.fitness.server.agent.dto.SafetyDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Safety Policy - 最小干预原则
 * F1-A重构：只检测极端危险行为，其他交给LLM判断
 * 
 * 设计理念：
 * - LLM比关键词匹配更智能，应该信任LLM的判断能力
 * - SafetyPolicy只保留必须阻断的极端情况（自残、严重危险行为）
 * - 疾病、疼痛等由LLM在prompt中处理，提供专业建议但不阻断
 */
@Component
public class SafetyPolicy {
    
    private static final Logger logger = LoggerFactory.getLogger(SafetyPolicy.class);
    
    // 极端危险行为和训练安全高风险关键词均禁止草案。
    private static final List<String> BLOCK_KEYWORDS = Arrays.asList(
        "自残", "自杀", "轻生", "结束生命", "不想活", "绝食", "断食超过", "不吃东西", "饿死",
        "疼痛", "受伤", "伤病", "骨折", "疾病", "高血压", "心脏病", "药物", "处方",
        "危险补剂", "类固醇", "昏厥", "胸痛", "呼吸困难"
    );
    
    /**
     * 前置安全检查
     * 只检测极端危险行为，其他交给LLM判断
     */
    public SafetyDecision preCheck(String userMessage) {
        SafetyDecision decision = new SafetyDecision();
        decision.setRestrictions(new ArrayList<>());
        decision.setForbiddenActions(new ArrayList<>());
        
        String lowerMsg = userMessage.toLowerCase();
        
        // F1-A 安全边界：高风险训练输入不生成任何草案候选。
        if (containsAny(lowerMsg, BLOCK_KEYWORDS)) {
            decision.setRiskLevel(SafetyDecision.RiskLevel.BLOCK);
            decision.setBlockDraft(true);
            decision.getRestrictions().add("检测到需要先进行安全评估的健康风险内容");
            decision.setResponseGuidance("暂不生成训练计划草案，请先停止可能加重风险的训练，并咨询医生或专业教练");
            decision.setEscalationAdvice("如出现胸痛、呼吸困难、昏厥或紧急症状，请立即就医");
            logger.warn("Safety check blocked high-risk training request");
            return decision;
        }

        decision.setRiskLevel(SafetyDecision.RiskLevel.SAFE);
        decision.setBlockDraft(false);
        
        return decision;
    }
    
    /**
     * 草案生成前复核
     */
    public SafetyDecision reviewDraftCandidate(Object draftCandidate, SafetyDecision preCheckResult) {
        // F1-A: 简单复用preCheck结果
        // 后续可以增加对草案内容的检查
        return preCheckResult;
    }
    
    /**
     * 检查字符串是否包含列表中的任何关键词
     */
    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
