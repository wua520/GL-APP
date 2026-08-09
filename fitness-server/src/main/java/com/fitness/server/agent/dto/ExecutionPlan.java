package com.fitness.server.agent.dto;

import java.util.List;
import java.util.Map;

/**
 * Supervisor生成的执行计划
 * F1-A: Training垂直切片核心契约
 */
public class ExecutionPlan {
    
    private String taskId;
    private Long userId;  // 服务端注入，不来自模型
    private List<String> domains;  // TRAINING / NUTRITION / PROGRESS / KNOWLEDGE
    private ExecutionMode executionMode;
    private List<String> selectedAgents;
    private Map<String, List<String>> allowedToolsByAgent;  // agentName -> toolNames
    private RiskLevel riskLevel;
    private boolean draftAllowed;
    private long deadlineMs;
    private String routingReason;
    
    public enum ExecutionMode {
        SINGLE,      // 单领域
        PARALLEL,    // 跨域并行（只读）
        SEQUENTIAL   // 跨域串行
    }
    
    public enum RiskLevel {
        NORMAL,      // 正常请求
        CAUTION,     // 需要注意
        HIGH         // 高风险
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getDomains() {
        return domains;
    }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    public List<String> getSelectedAgents() {
        return selectedAgents;
    }

    public void setSelectedAgents(List<String> selectedAgents) {
        this.selectedAgents = selectedAgents;
    }

    public Map<String, List<String>> getAllowedToolsByAgent() {
        return allowedToolsByAgent;
    }

    public void setAllowedToolsByAgent(Map<String, List<String>> allowedToolsByAgent) {
        this.allowedToolsByAgent = allowedToolsByAgent;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isDraftAllowed() {
        return draftAllowed;
    }

    public void setDraftAllowed(boolean draftAllowed) {
        this.draftAllowed = draftAllowed;
    }

    public long getDeadlineMs() {
        return deadlineMs;
    }

    public void setDeadlineMs(long deadlineMs) {
        this.deadlineMs = deadlineMs;
    }

    public String getRoutingReason() {
        return routingReason;
    }

    public void setRoutingReason(String routingReason) {
        this.routingReason = routingReason;
    }

    /**
     * F1-B 执行边界：领域、委派对象和草案能力必须由服务端计划一一对应。
     */
    public void validate() {
        if (taskId == null || taskId.isBlank() || userId == null) {
            throw new IllegalArgumentException("执行计划缺少服务端任务身份");
        }
        if (domains == null || domains.isEmpty() || selectedAgents == null || selectedAgents.isEmpty()) {
            throw new IllegalArgumentException("执行计划缺少领域委派");
        }
        if (domains.size() != selectedAgents.size() || allowedToolsByAgent == null
                || allowedToolsByAgent.size() != selectedAgents.size()) {
            throw new IllegalArgumentException("执行计划领域、Agent与工具授权不一致");
        }

        Map<String, String> domainAgents = Map.of(
            "TRAINING", "TrainingAgent",
            "NUTRITION", "NutritionAgent",
            "PROGRESS", "ProgressAgent"
        );
        for (int index = 0; index < domains.size(); index++) {
            String domain = domains.get(index);
            String agentName = selectedAgents.get(index);
            if (!domainAgents.containsKey(domain) || !domainAgents.get(domain).equals(agentName)) {
                throw new IllegalArgumentException("执行计划包含未授权的领域委派");
            }
            List<String> grantedTools = allowedToolsByAgent.get(agentName);
            if (grantedTools == null || grantedTools.isEmpty()) {
                throw new IllegalArgumentException("领域任务缺少最小工具授权: " + domain);
            }
        }

        boolean grantsDraftTool = allowedToolsByAgent.values().stream()
            .flatMap(List::stream)
            .anyMatch(tool -> tool.equals("create_training_plan_draft") || tool.equals("create_diet_record_draft"));
        if (draftAllowed != grantsDraftTool) {
            throw new IllegalArgumentException("草案权限与工具授权不一致");
        }
        if (domains.size() == 1 && executionMode != ExecutionMode.SINGLE) {
            throw new IllegalArgumentException("单领域计划必须使用单任务执行模式");
        }
        if (domains.size() > 1 && executionMode == ExecutionMode.SINGLE) {
            throw new IllegalArgumentException("跨领域计划不能使用单任务执行模式");
        }
        if (domains.size() > 1 && grantsDraftTool && executionMode != ExecutionMode.SEQUENTIAL) {
            throw new IllegalArgumentException("含草案工具的跨领域计划必须串行执行");
        }
        if (!grantsDraftTool && executionMode == ExecutionMode.PARALLEL && domains.size() < 2) {
            throw new IllegalArgumentException("并行执行至少需要两个只读领域任务");
        }
        if (deadlineMs <= System.currentTimeMillis()) {
            throw new IllegalArgumentException("执行计划已超时");
        }
        if (riskLevel == null || routingReason == null || routingReason.isBlank()) {
            throw new IllegalArgumentException("执行计划缺少审计字段");
        }
    }
}
