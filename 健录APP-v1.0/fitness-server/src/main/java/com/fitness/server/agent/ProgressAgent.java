package com.fitness.server.agent;

import com.fitness.server.agent.dto.AgentResultDto;
import com.fitness.server.agent.dto.AgentTaskDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Progress Agent - 趋势分析专家
 * F1-B: 分析体重/围度趋势、目标进展（只读，无草案能力）
 */
@Component
public class ProgressAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(ProgressAgent.class);
    
    @Autowired
    private AgentToolExecutorV2 toolExecutor;
    
    @Autowired
    private LlmClient llmClient;

    @Autowired
    private AuthorizedToolResolver authorizedToolResolver;
    
    private static final String DOMAIN_OWNER = "progress";
    
    /**
     * 执行趋势分析任务
     */
    public AgentResultDto execute(AgentTaskDto task, Long userId) {
        logger.info("ProgressAgent executing task: {}", task.getTaskId());
        
        AgentResultDto result = new AgentResultDto();
        result.setTaskId(task.getTaskId());
        result.setAgentName("ProgressAgent");
        result.setFacts(new ArrayList<>());
        result.setFindings(new ArrayList<>());
        result.setConstraints(new ArrayList<>());
        result.setEvidenceRefs(new ArrayList<>());
        
        try {
            // 验证工具权限
            if (!validateToolPermissions(task.getAllowedToolNames())) {
                result.setStatus(AgentResultDto.ResultStatus.FAILED);
                result.setFailureReason("ProgressAgent received unauthorized tools");
                logger.error("Tool permission validation failed for task: {}", task.getTaskId());
                return result;
            }
            
            // 调用LLM执行趋势分析
            String analysis = analyzeProgressRequest(task, userId);
            
            // 提炼为结构化结果
            result.getFindings().add(analysis);
            
            // 提取约束和限制
            extractConstraints(analysis, result);
            
            // 检查是否包含错误信息
            if (analysis.contains("暂时不可用") || analysis.contains("分析超时")) {
                result.setStatus(AgentResultDto.ResultStatus.PARTIAL);
                result.setConfidence(0.3);
            } else if (analysis.contains("数据不足") || analysis.contains("无法判断")) {
                result.setStatus(AgentResultDto.ResultStatus.PARTIAL);
                result.setConfidence(0.5);
            } else {
                result.setStatus(AgentResultDto.ResultStatus.SUCCESS);
                result.setConfidence(0.7);  // 趋势分析置信度略低
            }
            
        } catch (Exception e) {
            logger.error("ProgressAgent execution failed: {}", e.getMessage(), e);
            result.setStatus(AgentResultDto.ResultStatus.FAILED);
            result.setFailureReason(e.getMessage());
            result.setConfidence(0.0);
        }
        
        return result;
    }
    
    /**
     * 从分析文本中提取约束和限制
     */
    private void extractConstraints(String analysis, AgentResultDto result) {
        String lowerAnalysis = analysis.toLowerCase();
        
        if (lowerAnalysis.contains("数据不足") || lowerAnalysis.contains("样本太少")) {
            result.getConstraints().add("数据样本不足，需要更长时间观察");
        }
        
        if (lowerAnalysis.contains("短期波动") || lowerAnalysis.contains("正常波动")) {
            result.getConstraints().add("可能是短期波动，不代表真实趋势");
        }
        
        if (lowerAnalysis.contains("平台期") && lowerAnalysis.contains("可能")) {
            result.getConstraints().add("可能进入平台期，但需要更多数据确认");
        }
        
        if (lowerAnalysis.contains("无法判断") || lowerAnalysis.contains("不确定")) {
            result.getConstraints().add("当前数据无法做出确定结论");
        }
    }
    
    /**
     * 进度分析可读取跨领域事实，但不能获得任何草案工具。
     */
    private boolean validateToolPermissions(List<String> requestedTools) {
        try {
            authorizedToolResolver.validateAuthorization(
                requestedTools,
                java.util.Set.of(DOMAIN_OWNER, "training", "nutrition"),
                false
            );
            return true;
        } catch (IllegalArgumentException e) {
            logger.warn("ProgressAgent received invalid tool authorization: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 分析趋势请求
     */
    private String analyzeProgressRequest(AgentTaskDto task, Long userId) {
        try {
            String systemPrompt = buildProgressAgentSystemPrompt(task);
            
            List<LlmClient.Message> messages = new ArrayList<>();
            messages.add(new LlmClient.Message("system", systemPrompt));
            messages.add(new LlmClient.Message("user", task.getUserQuestion()));
            
            List<LlmClient.Tool> allowedTools = convertToLlmTools(task.getAllowedToolNames());
            
            int maxIterations = 5;
            int iteration = 0;
            String finalResponse = null;
            
            while (iteration < maxIterations) {
                iteration++;
                logger.debug("ProgressAgent iteration {}/{}", iteration, maxIterations);
                
                LlmClient.LlmResponse llmResponse = llmClient.chat(messages, allowedTools);
                
                if (llmResponse.hasToolCalls()) {
                    if (!"tool_calls".equals(llmResponse.getFinishReason())) {
                        throw new RuntimeException("LLM工具调用缺少tool_calls终止原因");
                    }
                    logger.info("ProgressAgent detected {} tool calls", llmResponse.getToolCalls().size());
                    
                    messages.add(new LlmClient.Message("assistant", llmResponse.getContent(), llmResponse.getToolCalls()));
                    
                    for (LlmClient.ToolCall toolCall : llmResponse.getToolCalls()) {
                        String toolResult;
                        
                        try {
                            if (!isToolAllowed(toolCall.getName(), task.getAllowedToolNames())) {
                                toolResult = ToolResultJson.error(
                                    "unauthorized_tool",
                                    "工具不在当前请求的授权范围内: " + toolCall.getName(),
                                    false
                                );
                                logger.warn("ProgressAgent attempted unauthorized tool: {}", toolCall.getName());
                            } else {
                                toolResult = toolExecutor.executeTool(
                                    userId,
                                    task.getAllowedToolNames(),
                                    toolCall.getName(),
                                    toolCall.getArguments()
                                );
                                logger.info("ProgressAgent executed tool: {}", toolCall.getName());
                            }
                        } catch (AgentToolExecutorV2.ToolExecutionException e) {
                            toolResult = ToolResultJson.error(e.getCode(), e.getMessage(), e.isRetryable());
                            logger.error("Tool execution failed: {}", e.getMessage(), e);
                        }
                        
                        messages.add(new LlmClient.Message("tool", toolResult, toolCall.getId()));
                    }
                    
                    continue;
                }
                
                if (!"stop".equals(llmResponse.getFinishReason())) {
                    throw new RuntimeException("LLM未正常完成回复，终止原因: " + llmResponse.getFinishReason());
                }
                if (llmResponse.getContent() != null && !llmResponse.getContent().isBlank()) {
                    finalResponse = llmResponse.getContent();
                    break;
                }
                
                logger.error("ProgressAgent received invalid LLM response");
                throw new RuntimeException("LLM返回了无效响应");
            }
            
            if (finalResponse == null) {
                logger.warn("ProgressAgent reached max iterations without final response");
                return "分析超时，请稍后重试";
            }
            
            return finalResponse;
            
        } catch (Exception e) {
            logger.error("Progress analysis failed: {}", e.getMessage(), e);
            return "趋势分析暂时不可用：" + e.getMessage();
        }
    }
    
    private boolean isToolAllowed(String toolName, List<String> allowedToolNames) {
        if (allowedToolNames == null) {
            return false;
        }
        return allowedToolNames.contains(toolName);
    }
    
    /**
     * 构建Progress Agent专用系统提示
     */
    private String buildProgressAgentSystemPrompt(AgentTaskDto task) {
        return String.format("""
            你是健录App的趋势分析专家Agent。
            
            职责范围：
            - 分析用户的体重/围度趋势（14-28天窗口）
            - 评估训练和饮食目标完成进度
            - 识别平台期风险
            
            严格限制：
            - 只能使用授权的只读工具
            - 不能生成任何草案或修改数据
            - 必须区分"证据不足"、"短期波动"和"存在趋势"
            - 不能仅凭数天体重变化断言平台期
            - 不能提供医疗建议
            
            分析原则：
            - 至少需要7天数据才能初步分析
            - 14天以上数据才能较为可信
            - 明确指出数据局限性
            - 区分相关性和因果性
            
            当前任务目标：%s
            授权工具：%s
            
            请基于数据客观分析，明确说明分析的置信度和局限性。
            """, 
            task.getObjective(),
            task.getAllowedToolNames() != null ? String.join(", ", task.getAllowedToolNames()) : "无"
        );
    }
    
    /**
     * 将授权工具名解析为统一注册的模型工具定义。
     */
    private List<LlmClient.Tool> convertToLlmTools(List<String> toolNames) {
        return authorizedToolResolver.resolve(toolNames);
    }
    
}
