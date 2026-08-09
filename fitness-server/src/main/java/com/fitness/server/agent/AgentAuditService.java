package com.fitness.server.agent;

import com.fitness.server.entity.AgentAuditLog;
import com.fitness.server.mapper.AgentAuditLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Agent 审计服务
 * 负责记录Agent执行过程的审计日志
 * 
 * 职责：
 * 1. 记录任务的每个执行步骤
 * 2. 记录工具调用的输入输出
 * 3. 数据脱敏，保护隐私
 */
@Service
public class AgentAuditService {
    
    @Autowired
    private AgentAuditLogMapper auditLogMapper;
    
    /**
     * 记录分析步骤
     */
    public void logAnalyzing(Long taskId) {
        AgentAuditLog log = new AgentAuditLog();
        log.setTaskId(taskId);
        log.setStepType("ANALYZING");
        log.setCreatedAt(System.currentTimeMillis());
        
        auditLogMapper.insert(log);
    }
    
    /**
     * 记录工具调用
     * 
     * @param taskId 任务ID
     * @param toolName 工具名称
     * @param inputJson 输入参数JSON
     * @param outputJson 输出结果JSON
     */
    public void logToolCall(Long taskId, String toolName, String inputJson, String outputJson) {
        AgentAuditLog log = new AgentAuditLog();
        log.setTaskId(taskId);
        log.setStepType("TOOL_CALL");
        log.setToolName(toolName);
        log.setInputSummary(sanitizeToolInput(toolName, inputJson));
        log.setOutputSummary(sanitizeToolOutput(toolName, outputJson));
        log.setCreatedAt(System.currentTimeMillis());
        
        auditLogMapper.insert(log);
    }
    
    /**
     * 通用日志记录
     */
    public void log(Long taskId, String stepType, String toolName, String inputSummary, String outputSummary) {
        AgentAuditLog log = new AgentAuditLog();
        log.setTaskId(taskId);
        log.setStepType(stepType);
        log.setToolName(toolName);
        log.setInputSummary(inputSummary);
        log.setOutputSummary(outputSummary);
        log.setCreatedAt(System.currentTimeMillis());
        
        auditLogMapper.insert(log);
    }
    
    /**
     * 记录审核知识实际进入用户回复的事实；不保存检索正文或用户原始输入。
     */
    public void logKnowledgeHit(Long taskId, java.util.List<com.fitness.server.knowledge.KnowledgeCitation> citations) {
        String chunkIds = citations.stream()
            .map(citation -> String.valueOf(citation.chunkId()))
            .collect(java.util.stream.Collectors.joining(","));
        log(taskId, "KNOWLEDGE_HIT", "KnowledgeAgent", "审核分块: " + chunkIds,
            "引用数: " + citations.size());
    }
    /**
     * 记录响应生成步骤
     */
    public void logResponding(Long taskId) {
        AgentAuditLog log = new AgentAuditLog();
        log.setTaskId(taskId);
        log.setStepType("RESPONDING");
        log.setCreatedAt(System.currentTimeMillis());
        
        auditLogMapper.insert(log);
    }
    
    /**
     * 工具输入脱敏
     * 只记录参数名称和类型，不记录具体值
     */
    private String sanitizeToolInput(String toolName, String inputJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(inputJson);
            
            StringBuilder summary = new StringBuilder();
            summary.append("工具=").append(toolName).append(", 参数={");
            
            node.fieldNames().forEachRemaining(fieldName -> {
                com.fasterxml.jackson.databind.JsonNode value = node.get(fieldName);
                summary.append(fieldName).append("=")
                       .append(value.isNumber() ? "数字" : 
                               value.isTextual() ? "文本" : 
                               value.getNodeType().toString())
                       .append(", ");
            });
            
            if (summary.toString().endsWith(", ")) {
                summary.setLength(summary.length() - 2);
            }
            summary.append("}");
            
            return summary.toString();
        } catch (Exception e) {
            return "工具=" + toolName + ", 参数解析失败";
        }
    }
    
    /**
     * 工具输出脱敏
     * 只记录记录数量和字段数量，不记录具体内容
     */
    private String sanitizeToolOutput(String toolName, String outputJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(outputJson);
            
            StringBuilder summary = new StringBuilder();
            summary.append("工具=").append(toolName).append(", ");
            
            // 统计字段数量
            int fieldCount = 0;
            node.fieldNames().forEachRemaining(field -> {});
            summary.append("字段数=").append(countFields(node)).append(", ");
            
            // 统计数组记录数
            int arrayCount = countArrayElements(node);
            if (arrayCount > 0) {
                summary.append("记录数=").append(arrayCount);
            } else {
                summary.append("类型=对象");
            }
            
            return summary.toString();
        } catch (Exception e) {
            return "工具=" + toolName + ", 输出解析失败";
        }
    }
    
    /**
     * 统计JSON字段数量
     */
    private int countFields(com.fasterxml.jackson.databind.JsonNode node) {
        int count = 0;
        if (node.isObject()) {
            java.util.Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                fieldNames.next();
                count++;
            }
        }
        return count;
    }
    
    /**
     * 统计数组元素数量
     */
    private int countArrayElements(com.fasterxml.jackson.databind.JsonNode node) {
        if (node.isObject()) {
            for (com.fasterxml.jackson.databind.JsonNode value : node) {
                if (value.isArray()) {
                    return value.size();
                }
            }
        }
        return 0;
    }
    
    /**
     * 数据脱敏（旧方法，保留兼容）
     */
    private String sanitize(String data) {
        if (data == null || data.length() <= 100) {
            return data;
        }
        
        // 只保留前100字符
        return data.substring(0, 100) + "...[已脱敏]";
    }
    
    /**
     * F1-A: 记录Supervisor规划步骤
     */
    public void logSupervisorPlanning(Long taskId, String domains, String selectedAgents) {
        AgentAuditLog log = new AgentAuditLog();
        log.setTaskId(taskId);
        log.setStepType("SUPERVISOR_PLANNING");
        log.setInputSummary("领域: " + domains);
        log.setOutputSummary("选中Agent: " + selectedAgents);
        log.setCreatedAt(System.currentTimeMillis());
        
        auditLogMapper.insert(log);
    }
    
    /**
     * F1-A: 记录安全检查步骤
     */
    public void logSafetyCheck(Long taskId, String riskLevel, boolean blockDraft) {
        AgentAuditLog log = new AgentAuditLog();
        log.setTaskId(taskId);
        log.setStepType("SAFETY_CHECK");
        log.setInputSummary("风险等级: " + riskLevel);
        log.setOutputSummary("阻断草案: " + blockDraft);
        log.setCreatedAt(System.currentTimeMillis());
        
        auditLogMapper.insert(log);
    }
    
    /**
     * F1-A: 记录Agent执行步骤
     */
    public void logAgentExecution(Long taskId, String agentName, String status, String findings) {
        AgentAuditLog log = new AgentAuditLog();
        log.setTaskId(taskId);
        log.setStepType("AGENT_EXECUTION");
        log.setToolName(agentName);
        log.setInputSummary("Agent: " + agentName);
        log.setOutputSummary("状态: " + status + ", 发现: " + 
            (findings != null && findings.length() > 50 ? findings.substring(0, 50) + "..." : findings));
        log.setCreatedAt(System.currentTimeMillis());
        
        auditLogMapper.insert(log);
    }
}
