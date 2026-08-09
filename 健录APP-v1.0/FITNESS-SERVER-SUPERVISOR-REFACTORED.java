package com.fitness.server.agent;

import com.fitness.server.agent.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Supervisor - 任务编排核心（重构版 F1-A）
 * 
 * 职责：
 * 1. 生成 ExecutionPlan（领域路由、工具分配）
 * 2. 委派任务给 Agent
 * 3. 聚合 AgentResult
 * 4. 不直接执行工具
 * 5. 不直接调用 LLM
 * 6. 不包含确定性路径逻辑（移到Agent内部）
 * 
 * F1-A 重构：清晰的职责边界
 */
@Service
public class SupervisorService {
    
    private static final Logger logger = LoggerFactory.getLogger(SupervisorService.class);
    
    @Autowired
    private SafetyPolicy safetyPolicy;
    
    @Autowired
    private TrainingAgent trainingAgent;
    
    @Autowired
    private NutritionAgent nutritionAgent;
    
    @Autowired
    private ProgressAgent progressAgent;
    
    /**
     * 规划并执行任务（重构版）
     * 
     * 流程：
     * 1. 安全前置检查
     * 2. 生成执行计划（确定性路由）
     * 3. 执行 Agent
     * 4. 安全复核
     * 5. 聚合结果
     */
    public SupervisorResult planAndExecute(String userMessage, Long userId, String sessionId) {
        logger.info("Supervisor planning task for user: {}, session: {}", userId, sessionId);
        
        String taskId = UUID.randomUUID().toString();
        SupervisorResult result = new SupervisorResult();
        result.setTaskId(taskId);
        
        try {
            // 1. 安全前置检查
            SafetyDecision safetyDecision = safetyPolicy.preCheck(userMessage);
            result.setSafetyDecision(safetyDecision);
            
            // 如果高风险，阻断执行
            if (safetyDecision.getRiskLevel() == SafetyDecision.RiskLevel.HIGH || 
                safetyDecision.getRiskLevel() == SafetyDecision.RiskLevel.BLOCK) {
                logger.warn("Task blocked by safety policy: {} - {}", taskId, safetyDecision.getRiskLevel());
                result.setStatus("BLOCKED");
                result.setFinalMessage(buildSafetyBlockedMessage(safetyDecision));
                return result;
            }
            
            // 2. 生成执行计划
            ExecutionPlan plan = generateExecutionPlan(userMessage, userId, taskId, safetyDecision);
            result.setExecutionPlan(plan);
            
            // 如果未识别领域，降级
            if (plan.getDomains().isEmpty() || plan.getDomains().contains("UNKNOWN")) {
                logger.warn("Unknown domain detected: {}", taskId);
                result.setStatus("FALLBACK");
                result.setFinalMessage("当前版本只支持训练、饮食、进度分析领域，其他领域即将支持。");
                return result;
            }
            
            // 3. 执行 Agent（按计划）
            List<AgentResultDto> agentResults = executeAgents(plan, userMessage, userId);
            result.setAgentResults(agentResults);
            
            // 4. 安全复核草案
            reviewDrafts(agentResults, safetyDecision);
            
            // 5. 聚合结果
            String finalMessage = aggregateResults(agentResults, safetyDecision);
            result.setFinalMessage(finalMessage);
            
            // 6. 确定最终状态
            result.setStatus(determineOverallStatus(agentResults));
            
            logger.info("Supervisor completed: taskId={}, status={}", taskId, result.getStatus());
            
        } catch (Exception e) {
            logger.error("Supervisor execution failed: taskId={}", taskId, e);
            result.setStatus("FAILED");
            result.setFinalMessage("执行失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 生成执行计划（确定性规则）
     */
    private ExecutionPlan generateExecutionPlan(String userMessage, Long userId, 
                                                String taskId, SafetyDecision safetyDecision) {
        ExecutionPlan plan = new ExecutionPlan();
        plan.setTaskId(taskId);
        plan.setUserId(userId);
        plan.setDeadlineMs(System.currentTimeMillis() + 30000);  // 30秒超时
        
        String lowerMsg = userMessage.toLowerCase();
        List<String> domains = new ArrayList<>();
        List<String> selectedAgents = new ArrayList<>();
        Map<String, List<String>> toolsByAgent = new HashMap<>();
        
        // 检测领域
        boolean hasTraining = containsTrainingDomain(lowerMsg);
        boolean hasNutrition = containsNutritionDomain(lowerMsg);
        boolean hasProgress = containsProgressDomain(lowerMsg);
        
        // 训练领域
        if (hasTraining) {
            domains.add("TRAINING");
            selectedAgents.add("TrainingAgent");
            toolsByAgent.put("TrainingAgent", getTrainingTools(lowerMsg, safetyDecision));
        }
        
        // 饮食领域
        if (hasNutrition) {
            domains.add("NUTRITION");
            selectedAgents.add("NutritionAgent");
            toolsByAgent.put("NutritionAgent", getNutritionTools(lowerMsg, safetyDecision));
        }
        
        // 进度领域
        if (hasProgress) {
            domains.add("PROGRESS");
            selectedAgents.add("ProgressAgent");
            toolsByAgent.put("ProgressAgent", getProgressTools());
        }
        
        // 设置路由原因
        if (domains.isEmpty()) {
            domains.add("UNKNOWN");
            plan.setRoutingReason("未识别领域");
        } else if (domains.size() == 1) {
            plan.setRoutingReason("单领域任务：" + domains.get(0));
        } else {
            plan.setRoutingReason("跨领域任务：" + String.join(" + ", domains));
        }
        
        plan.setDomains(domains);
        plan.setSelectedAgents(selectedAgents);
        plan.setExecutionMode(domains.size() > 1 ? 
            ExecutionPlan.ExecutionMode.SEQUENTIAL : ExecutionPlan.ExecutionMode.SINGLE);
        plan.setAllowedToolsByAgent(toolsByAgent);
        plan.setDraftAllowed(!safetyDecision.isBlockDraft());
        plan.setRiskLevel(mapSafetyRiskToExecutionRisk(safetyDecision.getRiskLevel()));
        
        return plan;
    }
    
    /**
     * 执行 Agent（按执行计划）
     */
    private List<AgentResultDto> executeAgents(ExecutionPlan plan, String userMessage, Long userId) {
        List<AgentResultDto> results = new ArrayList<>();
        long deadlineMs = plan.getDeadlineMs();
        
        for (String agentName : plan.getSelectedAgents()) {
            // 检查超时
            if (System.currentTimeMillis() >= deadlineMs) {
                logger.warn("Timeout before executing {}", agentName);
                results.add(createTimeoutResult(agentName));
                continue;
            }
            
            // 创建 Agent 任务
            AgentTaskDto task = createAgentTask(plan, agentName, userMessage);
            
            // 执行 Agent
            try {
                AgentResultDto result = switch (agentName) {
                    case "TrainingAgent" -> trainingAgent.execute(task, userId);
                    case "NutritionAgent" -> nutritionAgent.execute(task, userId);
                    case "ProgressAgent" -> progressAgent.execute(task, userId);
                    default -> createFailedResult(agentName, "Unknown agent");
                };
                results.add(result);
                
            } catch (Exception e) {
                logger.error("Agent {} execution failed", agentName, e);
                results.add(createFailedResult(agentName, e.getMessage()));
            }
        }
        
        return results;
    }
    
    /**
     * 安全复核草案
     */
    private void reviewDrafts(List<AgentResultDto> agentResults, SafetyDecision preCheckResult) {
        for (AgentResultDto result : agentResults) {
            if (result.getDraftCandidate() != null) {
                SafetyDecision draftReview = safetyPolicy.reviewDraftCandidate(
                    result.getDraftCandidate(), 
                    preCheckResult
                );
                
                if (draftReview.isBlockDraft()) {
                    logger.warn("Draft blocked by safety review: {}", result.getAgentName());
                    result.setDraftCandidate(null);
                    result.getConstraints().add("草案被安全策略阻断");
                }
            }
        }
    }
    
    /**
     * 聚合结果
     */
    private String aggregateResults(List<AgentResultDto> agentResults, SafetyDecision safetyDecision) {
        StringBuilder message = new StringBuilder();
        
        // 安全约束优先
        if (safetyDecision.getRestrictions() != null && !safetyDecision.getRestrictions().isEmpty()) {
            message.append("【安全提示】\n");
            for (String restriction : safetyDecision.getRestrictions()) {
                message.append("- ").append(restriction).append("\n");
            }
            message.append("\n");
        }
        
        // Agent 结论
        for (AgentResultDto result : agentResults) {
            message.append("【").append(result.getAgentName()).append("】\n");
            if (result.getFindings() != null) {
                for (String finding : result.getFindings()) {
                    message.append(finding).append("\n");
                }
            }
            message.append("\n");
        }
        
        return message.toString().trim();
    }
    
    /**
     * 确定整体状态
     */
    private String determineOverallStatus(List<AgentResultDto> agentResults) {
        if (agentResults.isEmpty()) {
            return "FAILED";
        }
        
        boolean allFailed = agentResults.stream()
            .allMatch(r -> r.getStatus() == AgentResultDto.ResultStatus.FAILED);
        
        boolean hasPartialOrFailed = agentResults.stream()
            .anyMatch(r -> r.getStatus() == AgentResultDto.ResultStatus.PARTIAL || 
                          r.getStatus() == AgentResultDto.ResultStatus.FAILED);
        
        if (allFailed) {
            return "FAILED";
        } else if (hasPartialOrFailed) {
            return "PARTIAL";
        } else {
            return "SUCCESS";
        }
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 获取训练领域工具
     */
    private List<String> getTrainingTools(String lowerMsg, SafetyDecision safety) {
        List<String> tools = new ArrayList<>();
        tools.add("get_training_summary");
        tools.add("get_recent_workouts");
        tools.add("get_active_training_plan");
        tools.add("get_training_schedule");
        tools.add("get_training_progress");
        
        // 如果有创建意图且安全允许，添加draft工具
        if (!safety.isBlockDraft() && containsTrainingPlanCreationIntent(lowerMsg)) {
            tools.add("create_training_plan_draft");
        }
        
        return tools;
    }
    
    /**
     * 获取饮食领域工具
     */
    private List<String> getNutritionTools(String lowerMsg, SafetyDecision safety) {
        List<String> tools = new ArrayList<>();
        tools.add("get_diet_summary");
        tools.add("get_daily_nutrition_progress");
        tools.add("get_today_diet_summary");
        tools.add("get_user_fitness_profile");
        
        // 如果有记录意图且安全允许，添加draft工具
        if (!safety.isBlockDraft() && containsDietRecordCreationIntent(lowerMsg)) {
            tools.add("create_diet_record_draft");
        }
        
        return tools;
    }
    
    /**
     * 获取进度领域工具
     */
    private List<String> getProgressTools() {
        List<String> tools = new ArrayList<>();
        tools.add("get_body_trend");
        tools.add("get_training_progress");
        tools.add("get_diet_summary");
        tools.add("get_daily_nutrition_progress");
        tools.add("get_user_fitness_profile");
        return tools;
    }
    
    /**
     * 检测训练领域
     */
    private boolean containsTrainingDomain(String lowerMsg) {
        return lowerMsg.contains("训练") || lowerMsg.contains("健身") || 
               lowerMsg.contains("锻炼") || lowerMsg.contains("力量") || 
               lowerMsg.contains("计划");
    }
    
    /**
     * 检测饮食领域
     */
    private boolean containsNutritionDomain(String lowerMsg) {
        return lowerMsg.contains("饮食") || lowerMsg.contains("吃") || 
               lowerMsg.contains("营养") || lowerMsg.contains("食物") ||
               lowerMsg.contains("餐") || lowerMsg.contains("热量") || 
               lowerMsg.contains("蛋白质");
    }
    
    /**
     * 检测进度领域
     */
    private boolean containsProgressDomain(String lowerMsg) {
        return lowerMsg.contains("进度") || lowerMsg.contains("效果") || 
               lowerMsg.contains("趋势") || lowerMsg.contains("变化") ||
               lowerMsg.contains("平台期");
    }
    
    /**
     * 检测训练计划创建意图
     */
    private boolean containsTrainingPlanCreationIntent(String lowerMsg) {
        return (lowerMsg.contains("训练") || lowerMsg.contains("健身") || lowerMsg.contains("锻炼")) &&
               (lowerMsg.contains("计划") || lowerMsg.contains("方案") || 
                lowerMsg.contains("制定") || lowerMsg.contains("生成"));
    }
    
    /**
     * 检测饮食记录创建意图
     */
    private boolean containsDietRecordCreationIntent(String lowerMsg) {
        boolean hasExplicitConsumption = lowerMsg.contains("吃了") || lowerMsg.contains("喝了");
        boolean hasDietContext = lowerMsg.contains("饮食") || lowerMsg.contains("食物") ||
                                 lowerMsg.contains("餐") || lowerMsg.contains("早餐") ||
                                 lowerMsg.contains("午餐") || lowerMsg.contains("晚餐") ||
                                 lowerMsg.contains("加餐");
        boolean hasRecordAction = lowerMsg.contains("记录") || lowerMsg.contains("添加");
        
        return hasExplicitConsumption || (hasDietContext && hasRecordAction);
    }
    
    /**
     * 创建 Agent 任务
     */
    private AgentTaskDto createAgentTask(ExecutionPlan plan, String agentName, String userQuestion) {
        AgentTaskDto task = new AgentTaskDto();
        task.setTaskId(plan.getTaskId());
        task.setAgentName(agentName);
        task.setUserQuestion(userQuestion);
        task.setObjective(getAgentObjective(agentName));
        task.setAllowedToolNames(plan.getAllowedToolsByAgent().get(agentName));
        task.setContext(new HashMap<>());
        task.setDeadlineMs(plan.getDeadlineMs());
        return task;
    }
    
    /**
     * 获取 Agent 目标描述
     */
    private String getAgentObjective(String agentName) {
        return switch (agentName) {
            case "TrainingAgent" -> "分析训练相关需求，提供训练建议或生成训练计划";
            case "NutritionAgent" -> "分析饮食记录，提供营养建议或记录饮食";
            case "ProgressAgent" -> "分析健身进度和趋势，提供改进建议";
            default -> "分析用户需求并提供建议";
        };
    }
    
    /**
     * 创建超时结果
     */
    private AgentResultDto createTimeoutResult(String agentName) {
        AgentResultDto result = new AgentResultDto();
        result.setTaskId(UUID.randomUUID().toString());
        result.setAgentName(agentName);
        result.setStatus(AgentResultDto.ResultStatus.PARTIAL);
        result.setFacts(new ArrayList<>());
        result.setFindings(List.of(agentName + " 执行超时，请稍后重试"));
        result.setConstraints(List.of("执行超时"));
        result.setEvidenceRefs(new ArrayList<>());
        result.setConfidence(0.0);
        return result;
    }
    
    /**
     * 创建失败结果
     */
    private AgentResultDto createFailedResult(String agentName, String reason) {
        AgentResultDto result = new AgentResultDto();
        result.setTaskId(UUID.randomUUID().toString());
        result.setAgentName(agentName);
        result.setStatus(AgentResultDto.ResultStatus.FAILED);
        result.setFacts(new ArrayList<>());
        result.setFindings(List.of(agentName + " 执行失败"));
        result.setConstraints(new ArrayList<>());
        result.setEvidenceRefs(new ArrayList<>());
        result.setFailureReason(reason);
        result.setConfidence(0.0);
        return result;
    }
    
    /**
     * 构建安全阻断消息
     */
    private String buildSafetyBlockedMessage(SafetyDecision decision) {
        StringBuilder message = new StringBuilder();
        message.append("【安全提示】\n");
        
        if (decision.getRestrictions() != null) {
            for (String restriction : decision.getRestrictions()) {
                message.append("- ").append(restriction).append("\n");
            }
        }
        
        if (decision.getResponseGuidance() != null) {
            message.append("\n").append(decision.getResponseGuidance());
        }
        
        if (decision.getEscalationAdvice() != null) {
            message.append("\n\n").append(decision.getEscalationAdvice());
        }
        
        return message.toString();
    }
    
    /**
     * 映射安全风险到执行风险
     */
    private ExecutionPlan.RiskLevel mapSafetyRiskToExecutionRisk(SafetyDecision.RiskLevel safetyRisk) {
        return switch (safetyRisk) {
            case SAFE -> ExecutionPlan.RiskLevel.NORMAL;
            case CAUTION -> ExecutionPlan.RiskLevel.CAUTION;
            case HIGH, BLOCK -> ExecutionPlan.RiskLevel.HIGH;
        };
    }
    
    /**
     * Supervisor 结果
     */
    public static class SupervisorResult {
        private String taskId;
        private String status;  // SUCCESS / PARTIAL / BLOCKED / FALLBACK / FAILED
        private ExecutionPlan executionPlan;
        private SafetyDecision safetyDecision;
        private List<AgentResultDto> agentResults;
        private String finalMessage;
        
        // Getters and Setters
        public String getTaskId() {
            return taskId;
        }
        
        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public ExecutionPlan getExecutionPlan() {
            return executionPlan;
        }
        
        public void setExecutionPlan(ExecutionPlan executionPlan) {
            this.executionPlan = executionPlan;
        }
        
        public SafetyDecision getSafetyDecision() {
            return safetyDecision;
        }
        
        public void setSafetyDecision(SafetyDecision safetyDecision) {
            this.safetyDecision = safetyDecision;
        }
        
        public List<AgentResultDto> getAgentResults() {
            return agentResults;
        }
        
        public void setAgentResults(List<AgentResultDto> agentResults) {
            this.agentResults = agentResults;
        }
        
        public String getFinalMessage() {
            return finalMessage;
        }
        
        public void setFinalMessage(String finalMessage) {
            this.finalMessage = finalMessage;
        }
    }
}
