package com.fitness.server.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.server.agent.dto.AgentResultDto;
import com.fitness.server.agent.dto.AgentTaskDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Training Agent - 训练领域专家（重构版）
 * 
 * Phase F1-A 架构：
 * - 职责清晰：只处理训练领域任务
 * - 确定性路径：明确意图直接调用工具
 * - 结构化契约：AgentTask → AgentResult
 * - 最小工具集：只获得授权的训练工具
 * 
 * 不做：
 * - 不处理饮食、进度等其他领域
 * - 不自由决定工具权限
 * - 不直接写入数据库
 * - 不编造数据
 */
@Component
public class TrainingAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(TrainingAgent.class);
    
    @Autowired
    private AgentToolExecutorV2 toolExecutor;
    
    @Autowired
    private LlmClient llmClient;

    @Autowired
    private AuthorizedToolResolver authorizedToolResolver;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String DOMAIN_OWNER = "training";
    private static final String DRAFT_TOOL = "create_training_plan_draft";
    
    /**
     * 执行训练领域任务
     * 
     * @param task 任务描述（由Supervisor分配）
     * @param userId 用户ID（由Supervisor从JWT注入，不可信模型）
     * @return 结构化结果
     */
    public AgentResultDto execute(AgentTaskDto task, Long userId) {
        logger.info("TrainingAgent executing task: {} for user: {}", task.getTaskId(), userId);
        
        AgentResultDto result = initializeResult(task);
        
        try {
            // 1. 验证工具权限（防御性检查）
            if (!validateToolPermissions(task.getAllowedToolNames())) {
                return createFailedResult(task, "Unauthorized tools detected");
            }
            
            // 2. 检测确定性路径：训练计划创建
            if (isDeterministicPlanCreation(task)) {
                logger.info("Taking deterministic path: plan creation");
                return executeDeterministicPlanCreation(task, userId);
            }
            
            // 3. 其他场景：工具调用循环（有策略）
            logger.info("Taking LLM loop path: training analysis");
            return executeWithToolLoop(task, userId);
            
        } catch (Exception e) {
            logger.error("TrainingAgent execution failed", e);
            return createFailedResult(task, "Execution error: " + e.getMessage());
        }
    }
    
    /**
     * 初始化结果对象
     */
    private AgentResultDto initializeResult(AgentTaskDto task) {
        AgentResultDto result = new AgentResultDto();
        result.setTaskId(task.getTaskId());
        result.setAgentName("TrainingAgent");
        result.setFacts(new ArrayList<>());
        result.setFindings(new ArrayList<>());
        result.setConstraints(new ArrayList<>());
        result.setEvidenceRefs(new ArrayList<>());
        return result;
    }
    
    /**
     * 由统一工具契约验证任务授权，避免在 Agent 内重复维护工具白名单。
     */
    private boolean validateToolPermissions(List<String> requestedTools) {
        try {
            authorizedToolResolver.validateAuthorization(requestedTools, List.of(DOMAIN_OWNER), true);
            return true;
        } catch (IllegalArgumentException e) {
            logger.warn("TrainingAgent received invalid tool authorization: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检测确定性路径：训练计划创建意图
     * 
     * 满足条件：
     * 1. 被授权使用草案工具（关键条件）
     * 2. 用户问题包含训练/健身相关内容
     * 
     * 逻辑：如果Supervisor授权了draft工具，说明已经判断这是创建意图，
     *       TrainingAgent应该优先尝试生成草案
     */
    private boolean isDeterministicPlanCreation(AgentTaskDto task) {
        // 工具授权检测（关键条件）
        boolean hasDraftTool = task.getAllowedToolNames() != null && 
            task.getAllowedToolNames().contains(DRAFT_TOOL);
        
        if (!hasDraftTool) {
            return false;
        }
        return true;
    }
    
    /**
     * 确定性路径：训练计划创建
     * 
     * 不走LLM工具调用循环，直接调用草案工具生成结果
     * 保证100%生成草案（如果工具执行成功）
     */
    private AgentResultDto executeDeterministicPlanCreation(AgentTaskDto task, Long userId) {
        logger.info("Deterministic plan creation for task: {}", task.getTaskId());
        
        AgentResultDto result = initializeResult(task);
        
        try {
            // 从用户问题提取计划参数（简化版，实际应解析更丰富）
            Map<String, Object> planArgs = extractPlanArguments(task.getUserQuestion());
            
            // 直接调用草案工具
            String draftJson = toolExecutor.executeTool(
                userId,
                task.getAllowedToolNames(),
                DRAFT_TOOL,
                objectMapper.writeValueAsString(planArgs)
            );
            
            // 解析工具返回的normalized_draft_json
            JsonNode resultNode = objectMapper.readTree(draftJson);
            String normalizedDraft = resultNode.has("normalized_draft_json") ?
                resultNode.get("normalized_draft_json").asText() : null;
            
            if (normalizedDraft == null || normalizedDraft.isEmpty()) {
                // 草案工具未返回有效结果
                result.setStatus(AgentResultDto.ResultStatus.PARTIAL);
                result.getFindings().add("训练计划草案生成失败，工具未返回有效数据");
                result.setConfidence(0.3);
                logger.warn("Draft tool returned no valid normalized_draft_json");
                return result;
            }
            
            // 成功生成草案
            result.setStatus(AgentResultDto.ResultStatus.SUCCESS);
            result.getFacts().add(Map.of("key", "用户请求", "value", task.getUserQuestion()));
            result.getFacts().add(Map.of("key", "计划类型", "value", String.valueOf(planArgs.getOrDefault("planType", "未知"))));
            result.getFindings().add("已为你准备好训练计划草案，请确认后保存。");
            result.setDraftCandidate(normalizedDraft);
            result.setConfidence(0.9);
            result.getEvidenceRefs().add("tool:create_training_plan_draft");
            
            logger.info("Draft created successfully for task: {}", task.getTaskId());
            return result;
            
        } catch (AgentToolExecutorV2.ToolExecutionException e) {
            // 工具执行失败（业务层错误）
            logger.error("Tool execution failed: {}", e.getMessage());
            result.setStatus(AgentResultDto.ResultStatus.FAILED);
            result.setFailureReason("草案工具执行失败: " + e.getMessage());
            result.setConfidence(0.0);
            return result;
            
        } catch (Exception e) {
            // 技术层错误
            logger.error("Deterministic plan creation failed", e);
            result.setStatus(AgentResultDto.ResultStatus.FAILED);
            result.setFailureReason("技术错误: " + e.getMessage());
            result.setConfidence(0.0);
            return result;
        }
    }
    
    /**
     * 从用户问题提取训练计划参数
     * 
     * 简化实现：基于关键词匹配
     * 生产环境应使用更健壮的NLP或LLM提取
     */
    private Map<String, Object> extractPlanArguments(String userQuestion) {
        Map<String, Object> args = new LinkedHashMap<>();
        String lower = userQuestion.toLowerCase();
        String goal;
        String title;

        if (lower.contains("增肌") || lower.contains("增重")) {
            goal = "增肌";
            title = "增肌训练计划";
        } else if (lower.contains("减脂") || lower.contains("减肥")) {
            goal = "减脂";
            title = "减脂训练计划";
        } else if (lower.contains("力量")) {
            goal = "力量";
            title = "力量训练计划";
        } else {
            goal = "塑形";
            title = "综合训练计划";
        }

        int trainingDays = extractTrainingDays(lower);
        String experience = extractExperience(lower);

        args.put("title", title);
        args.put("description", "根据你的目标生成的每周" + trainingDays + "天训练安排");
        args.put("goal", goal);
        args.put("experience", experience);
        args.put("targetMuscles", "胸,背,腿,肩");
        args.put("trainingDays", trainingDays);
        args.put("trainingDuration", "60分钟");
        args.put("equipment", "健身房常用器械");
        args.put("days", buildTrainingDays(trainingDays, goal));
        return args;
    }

    private int extractTrainingDays(String lower) {
        for (int i = 1; i <= 7; i++) {
            if (lower.contains(i + "天") || lower.contains("每周" + i)) {
                return i;
            }
        }
        return 4;
    }

    private String extractExperience(String lower) {
        if (lower.contains("新手") || lower.contains("初学")) {
            return "新手";
        }
        if (lower.contains("高级") || lower.contains("专业")) {
            return "高级";
        }
        return "中级";
    }

    private List<Map<String, Object>> buildTrainingDays(int trainingDays, String goal) {
        List<Map<String, Object>> templates = List.of(
            trainingDay("胸部+三头", "杠铃卧推", "上斜哑铃卧推", "绳索下压"),
            trainingDay("背部+二头", "高位下拉", "坐姿划船", "哑铃弯举"),
            trainingDay("腿部", "深蹲", "罗马尼亚硬拉", "腿举"),
            trainingDay("肩部+核心", "哑铃推举", "侧平举", "平板支撑"),
            trainingDay("上肢综合", "俯卧撑", "单臂哑铃划船", "面拉"),
            trainingDay("下肢综合", "箭步蹲", "臀桥", "提踵"),
            trainingDay("全身训练", "硬拉", "哑铃推举", "卷腹")
        );
        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 0; i < trainingDays; i++) {
            Map<String, Object> template = new LinkedHashMap<>(templates.get(i));
            template.put("name", "第" + (i + 1) + "天");
            days.add(template);
        }
        return days;
    }

    private Map<String, Object> trainingDay(String focus, String first, String second, String third) {
        Map<String, Object> day = new LinkedHashMap<>();
        day.put("focus", focus);
        day.put("exercises", List.of(
            exercise(first, 4, "8-12次", "90秒"),
            exercise(second, 3, "10-12次", "90秒"),
            exercise(third, 3, "12-15次", "60秒")
        ));
        return day;
    }

    private Map<String, Object> exercise(String name, int sets, String reps, String restTime) {
        return Map.of(
            "name", name,
            "sets", sets,
            "reps", reps,
            "restTime", restTime,
            "notes", "动作全程可控"
        );
    }
    
    /**
     * 工具调用循环路径：训练分析和查询场景
     * 
     * 使用LLM工具调用能力，但有明确策略：
     * - 最多5轮
     * - 只能调用授权工具
     * - 工具结果验证
     * - 超时检测
     */
    private AgentResultDto executeWithToolLoop(AgentTaskDto task, Long userId) {
        logger.info("Tool loop execution for task: {}", task.getTaskId());
        
        AgentResultDto result = initializeResult(task);
        
        try {
            // 构建训练领域的系统提示
            String systemPrompt = buildTrainingAgentSystemPrompt(task);
            
            // 构建消息列表
            List<LlmClient.Message> messages = new ArrayList<>();
            messages.add(new LlmClient.Message("system", systemPrompt));
            messages.add(new LlmClient.Message("user", task.getUserQuestion()));
            
            // 转换授权工具为LLM格式
            List<LlmClient.Tool> allowedTools = convertToLlmTools(task.getAllowedToolNames());
            
            // 工具调用循环
            int maxIterations = 5;
            int iteration = 0;
            String finalResponse = null;
            List<String> toolsUsed = new ArrayList<>();
            
            long deadlineMs = task.getDeadlineMs();
            
            while (iteration < maxIterations) {
                iteration++;
                
                // 检查超时
                if (System.currentTimeMillis() > deadlineMs) {
                    logger.warn("Task {} exceeded deadline", task.getTaskId());
                    result.setStatus(AgentResultDto.ResultStatus.PARTIAL);
                    result.getFindings().add("分析超时，以下是部分结果");
                    result.setConfidence(0.5);
                    return result;
                }
                
                logger.debug("TrainingAgent iteration {}/{}", iteration, maxIterations);
                
                // 调用LLM
                LlmClient.LlmResponse llmResponse = llmClient.chat(messages, allowedTools);
                
                // 处理工具调用
                if (llmResponse.hasToolCalls()) {
                    if (!"tool_calls".equals(llmResponse.getFinishReason())) {
                        throw new RuntimeException("LLM工具调用缺少tool_calls终止原因");
                    }
                    logger.info("TrainingAgent: {} tool calls in iteration {}", 
                        llmResponse.getToolCalls().size(), iteration);
                    
                    // 添加assistant消息（包含tool_calls）
                    messages.add(new LlmClient.Message("assistant", 
                        llmResponse.getContent(), llmResponse.getToolCalls()));
                    
                    // 执行每个工具调用
                    for (LlmClient.ToolCall toolCall : llmResponse.getToolCalls()) {
                        String toolResult = executeToolCall(
                            toolCall, userId, task.getAllowedToolNames(), toolsUsed);
                        
                        // 添加工具结果到消息
                        messages.add(new LlmClient.Message("tool", toolResult, toolCall.getId()));
                    }
                    
                    // 继续下一轮
                    continue;
                }
                
                if (!"stop".equals(llmResponse.getFinishReason())) {
                    throw new RuntimeException("LLM未正常完成回复，终止原因: " + llmResponse.getFinishReason());
                }
                if (llmResponse.getContent() != null && !llmResponse.getContent().isBlank()) {
                    finalResponse = llmResponse.getContent();
                    break;
                }
                
                // 异常情况：既无工具调用也无内容
                logger.error("TrainingAgent: invalid LLM response at iteration {}", iteration);
                throw new RuntimeException("LLM返回了无效响应");
            }
            
            // 超出最大迭代次数
            if (finalResponse == null) {
                logger.warn("TrainingAgent reached max iterations without final response");
                result.setStatus(AgentResultDto.ResultStatus.PARTIAL);
                result.getFindings().add("分析达到最大轮次限制，请尝试简化问题");
                result.setConfidence(0.3);
                return result;
            }
            
            // 成功完成
            result.setStatus(AgentResultDto.ResultStatus.SUCCESS);
            result.getFindings().add(finalResponse);
            result.setConfidence(0.8);
            
            // 记录使用的工具作为证据
            for (String toolName : toolsUsed) {
                result.getEvidenceRefs().add("tool:" + toolName);
            }
            
            // 提取约束信息
            extractConstraintsFromResponse(finalResponse, result);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Tool loop execution failed", e);
            result.setStatus(AgentResultDto.ResultStatus.FAILED);
            result.setFailureReason("训练分析失败: " + e.getMessage());
            result.setConfidence(0.0);
            return result;
        }
    }
    
    /**
     * 执行单个工具调用
     */
    private String executeToolCall(
            LlmClient.ToolCall toolCall, 
            Long userId, 
            List<String> allowedToolNames,
            List<String> toolsUsed) {
        
        String toolName = toolCall.getName();
        
        try {
            // 再次验证工具授权（防御性）
            if (!isToolAllowed(toolName, allowedToolNames)) {
                logger.warn("TrainingAgent attempted unauthorized tool: {}", toolName);
                return ToolResultJson.error("unauthorized_tool", "工具不在当前请求的授权范围内: " + toolName, false);
            }
            
            // 执行工具
            // toolCall.getArguments()已经是JSON字符串，不需要再序列化
            Object args = toolCall.getArguments();
            String argsJson;
            if (args instanceof String) {
                argsJson = (String) args;
            } else {
                argsJson = objectMapper.writeValueAsString(args);
            }
            
            String result = toolExecutor.executeTool(
                userId,
                allowedToolNames,
                toolName,
                argsJson
            );
            
            logger.info("TrainingAgent executed tool: {}", toolName);
            toolsUsed.add(toolName);
            
            return result;
            
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error("Failed to serialize tool arguments: {}", e.getMessage());
            return ToolResultJson.error("argument_serialization_failed", e.getMessage(), false);
        } catch (AgentToolExecutorV2.ToolExecutionException e) {
            logger.error("Tool execution failed: {}", e.getMessage());
            return ToolResultJson.error(e.getCode(), e.getMessage(), e.isRetryable());
        }
    }
    
    /**
     * 检查工具是否在授权列表
     */
    private boolean isToolAllowed(String toolName, List<String> allowedToolNames) {
        if (allowedToolNames == null) {
            return false;
        }
        return allowedToolNames.contains(toolName);
    }
    
    /**
     * 从LLM回复中提取约束信息
     */
    private void extractConstraintsFromResponse(String response, AgentResultDto result) {
        String lower = response.toLowerCase();
        
        if (lower.contains("不建议") || lower.contains("避免")) {
            result.getConstraints().add("包含不建议的动作或方式");
        }
        
        if (lower.contains("休息") || lower.contains("恢复不足")) {
            result.getConstraints().add("需要注意恢复和休息");
        }
        
        if (lower.contains("数据不足") || lower.contains("无法判断")) {
            result.getConstraints().add("数据不足，分析可能不准确");
        }
        
        if (lower.contains("咨询医生") || lower.contains("专业指导") || 
            lower.contains("就医") || lower.contains("医疗")) {
            result.getConstraints().add("需要专业医疗或指导");
        }
    }
    
    /**
     * 构建Training Agent专用系统提示
     */
    private String buildTrainingAgentSystemPrompt(AgentTaskDto task) {
        return String.format("""
            你是健录App的训练领域专家Agent。
            
            你的职责范围：
            - 分析用户的训练数据和计划
            - 提供训练建议和恢复分析
            - 回答训练相关问题
            - **为各种身体状况的用户提供适合的训练建议**
            
            你的严格限制：
            - 只能使用授权的训练工具
            - 不能处理饮食、营养、身体数据趋势等其他领域问题
            - 不能提供医疗诊断或治疗建议
            - 不能编造数据，只能基于工具返回的真实数据
            
            **健康风险处理原则（重要）**：
            - 如用户提到疼痛/伤病/疾病，仍可生成训练计划
            - 在计划中包含"⚠️ 安全提示"，建议咨询医生/教练
            - 根据具体情况调整训练强度（如：低强度、避免特定动作）
            - 示例："膝盖疼痛" → 生成低冲击力训练 + 提示"建议先咨询医生确认膝关节健康"
            - 示例："高血压" → 生成适度有氧训练 + 提示"请在医生许可下进行，避免高强度间歇训练"
            - 极端情况（自残、严重危险）已被系统阻断，你不会收到这类请求
            
            当前任务目标：%s
            授权工具：%s
            
            工作原则：
            1. 按需调用工具（不要一次性获取所有数据）
            2. 基于数据给出具体建议
            3. 数据不足时明确说明
            4. 回复简洁专业
            5. **优先提供帮助，而非拒绝服务**
            """, 
            task.getObjective() != null ? task.getObjective() : "训练领域分析",
            task.getAllowedToolNames() != null ? String.join(", ", task.getAllowedToolNames()) : "无"
        );
    }
    
    /**
     * 将授权工具名解析为统一注册的模型工具定义。
     */
    private List<LlmClient.Tool> convertToLlmTools(List<String> toolNames) {
        return authorizedToolResolver.resolve(toolNames);
    }
    
    /**
     * 创建失败结果
     */
    private AgentResultDto createFailedResult(AgentTaskDto task, String reason) {
        AgentResultDto result = initializeResult(task);
        result.setStatus(AgentResultDto.ResultStatus.FAILED);
        result.setFailureReason(reason);
        result.setConfidence(0.0);
        return result;
    }
}
