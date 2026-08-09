package com.fitness.server.agent.dto;

import java.util.List;
import java.util.Map;

/**
 * Supervisor委派给子Agent的任务
 * F1-A: 结构化协作契约
 */
public class AgentTaskDto {
    
    private String taskId;
    private String agentName;
    private String userQuestion;
    private String objective;  // 该Agent的具体目标
    private List<String> allowedToolNames;
    private Map<String, Object> context;  // 最小必要结构化信息
    private long deadlineMs;

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

    public String getUserQuestion() {
        return userQuestion;
    }

    public void setUserQuestion(String userQuestion) {
        this.userQuestion = userQuestion;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public List<String> getAllowedToolNames() {
        return allowedToolNames;
    }

    public void setAllowedToolNames(List<String> allowedToolNames) {
        this.allowedToolNames = allowedToolNames;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public long getDeadlineMs() {
        return deadlineMs;
    }

    public void setDeadlineMs(long deadlineMs) {
        this.deadlineMs = deadlineMs;
    }
}
