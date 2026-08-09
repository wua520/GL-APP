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
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Training Agent 的最小工具集（READ工具）
    private static final List<String> READ_TOOLS = List.of(
        "get_training_summary",
        "get_recent_workouts",
        "get_active_training_plan",
        "get_training_schedule",
        "get_training_progress",
        "get_recovery_status"
    );
    
    // 唯一的草案工具
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
     * 验证工具权限：只能使用授权的训练工具
     */
    private boolean validateToolPermissions(List<String> requestedTools) {
        if (requestedTools == null || requestedTools.isEmpty()) {
            return true; // 无工具请求
        }
        
        List<String> allAllowedTools = new ArrayList<>(READ_TOOLS);
        allAllowedTools.add(DRAFT_TOOL);
        
        for (String tool : requestedTools) {
            if (!allAllowedTools.contains(tool)) {
                logger.warn("TrainingAgent: unauthorized tool requested: {}", tool);
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检测确定性路径：训练计划创建意图
     * 
     * 满足条件：
     * 1. 用户问题包含创建意图关键词
     * 2. 被授权使用草案工具
     */
    private boolean isDeterministicPlanCreation(AgentTaskDto task) {
        String lower = task.getUserQuestion().toLowerCase();
        
        // 创建意图检测
        boolean hasCreationIntent = 
            lower.contains("制定") || 
            lower.contains("生成") || 
            lower.contains("创建") || 
            lower.contains("训练计划") ||
            lower.contains("计划");
        
        // 工具授权检测
        boolean hasDraftTool = task.getAllowedToolNames() != null && 
            task.getAllowedToolNames().contains(DRAFT_TOOL);
        
        return hasCreationIntent && hasDraftTool;
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
            result.getFacts().add("用户请求：" + task.getUserQuestion());
            result.getFacts().add("计划类型：" + planArgs.getOrDefault("planType", "未知"));
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
        Map<String, Object> args = new HashMap<>();
        
        String lower = userQuestion.toLowerCase();
        
        // 提取计划类型
        if (lower.contains("增肌") || lower.contains("增重")) {
            args.put("planType", "muscle_building");
            args.put("planName", "增肌训练计划");
        } else if (lower.contains("减脂") || lower.contains("减肥")) {
            args.put("planType", "fat_loss");
            args.put("planName", "减脂训练计划");
        } else if (lower.contains("力量")) {
            args.put("planType", "strength");
            args.put("planName", "力量训练计划");
        } else {
            args.put("planType", "general");
            args.put("planName", "综合训练计划");
        }
        
        // 提取训练天数（优先级：明确数字 > 默认4天）
        int weeklyDays = 4; // 默认
        for (int i = 1; i <= 7; i++) {
            if (lower.contains(i + "天") || lower.contains("每周" + i)) {
                weeklyDays = i;
                break;
            }
        }
        args.put("weeklyDays", weeklyDays);
        
        // 提取经验水平
        if (lower.contains("新手") || lower.contains("初学")) {
            args.put("experience", "beginner");
        } else if (lower.contains("中级") || lower.contains("有经验")) {
            args.put("experience", "intermediate");
        } else if (lower.contains("高级") || lower.contains("专业")) {
            args.put("experience", "advanced");
        } else {
            args.put("experience", "intermediate"); // 默认
        }
        
        // 提取目标描述
        args.put("description", "根据您的需求定制");
        
        return args;
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
                
                // 如果没有工具调用，获取最终回复
                if (llmResponse.getContent() != null && !llmResponse.getContent().isEmpty()) {
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
                String error = String.format(
                    "{\"error\": \"工具 '%s' 不在授权范围内\"}", toolName);
                logger.warn("TrainingAgent attempted unauthorized tool: {}", toolName);
                return error;
            }
            
            // 执行工具
            String result = toolExecutor.executeTool(
                userId,
                allowedToolNames,
                toolName,
                toolCall.getArguments()
            );
            
            logger.info("TrainingAgent executed tool: {}", toolName);
            toolsUsed.add(toolName);
            
            return result;
            
        } catch (AgentToolExecutorV2.ToolExecutionException e) {
            // 工具执行失败
            String error = String.format(
                "{\"error\": \"工具执行失败\", \"reason\": \"%s\"}",
                e.getMessage()
            );
            logger.error("Tool execution failed: {}", e.getMessage());
            return error;
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
            
            你的严格限制：
            - 只能使用授权的训练工具
            - 不能处理饮食、营养、身体数据趋势等其他领域问题
            - 不能提供医疗诊断
            - 遇到疼痛、伤病问题，必须建议咨询专业医生
            - 不能编造数据，只能基于工具返回的真实数据
            
            当前任务目标：%s
            授权工具：%s
            
            工作原则：
            1. 按需调用工具（不要一次性获取所有数据）
            2. 基于数据给出具体建议
            3. 数据不足时明确说明
            4. 回复简洁专业
            """, 
            task.getObjective() != null ? task.getObjective() : "训练领域分析",
            task.getAllowedToolNames() != null ? String.join(", ", task.getAllowedToolNames()) : "无"
        );
    }
    
    /**
     * 转换工具名称为LLM Tool对象
     */
    private List<LlmClient.Tool> convertToLlmTools(List<String> toolNames) {
        List<LlmClient.Tool> tools = new ArrayList<>();
        
        if (toolNames == null) {
            return tools;
        }
        
        for (String toolName : toolNames) {
            LlmClient.Tool tool = createToolDefinition(toolName);
            if (tool != null) {
                tools.add(tool);
            }
        }
        
        return tools;
    }
    
    /**
     * 创建工具定义（简化版）
     */
    private LlmClient.Tool createToolDefinition(String toolName) {
        return switch (toolName) {
            case "get_training_summary" -> new LlmClient.Tool(
                "get_training_summary",
                "获取用户的训练摘要数据",
                createEmptyParametersSchema(),
                "READ"
            );
            case "get_recent_workouts" -> new LlmClient.Tool(
                "get_recent_workouts",
                "获取用户最近的训练记录",
                createDaysParameterSchema(),
                "READ"
            );
            case "get_active_training_plan" -> new LlmClient.Tool(
                "get_active_training_plan",
                "获取用户当前激活的训练计划",
                createEmptyParametersSchema(),
                "READ"
            );
            case "get_training_schedule" -> new LlmClient.Tool(
                "get_training_schedule",
                "获取用户的训练日程安排",
                createEmptyParametersSchema(),
                "READ"
            );
            case "get_training_progress" -> new LlmClient.Tool(
                "get_training_progress",
                "获取用户的训练进度统计",
                createEmptyParametersSchema(),
                "READ"
            );
            case "get_recovery_status" -> new LlmClient.Tool(
                "get_recovery_status",
                "获取用户的肌群恢复状态",
                createEmptyParametersSchema(),
                "READ"
            );
            case "create_training_plan_draft" -> new LlmClient.Tool(
                "create_training_plan_draft",
                "创建训练计划草案（需要用户确认）",
                createTrainingPlanDraftParameterSchema(),
                "DRAFT"
            );
            default -> {
                logger.warn("Unknown tool name: {}", toolName);
                yield null;
            }
        };
    }
    
    private Map<String, Object> createEmptyParametersSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", new HashMap<>());
        schema.put("required", new ArrayList<>());
        return schema;
    }
    
    private Map<String, Object> createDaysParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> daysProperty = new HashMap<>();
        daysProperty.put("type", "integer");
        daysProperty.put("description", "要获取最近多少天的训练记录");
        daysProperty.put("default", 7);
        properties.put("days", daysProperty);
        
        schema.put("properties", properties);
        schema.put("required", new ArrayList<>());
        return schema;
    }
    
    private Map<String, Object> createTrainingPlanDraftParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> planNameProperty = new HashMap<>();
        planNameProperty.put("type", "string");
        planNameProperty.put("description", "训练计划名称");
        properties.put("planName", planNameProperty);
        
        Map<String, Object> planTypeProperty = new HashMap<>();
        planTypeProperty.put("type", "string");
        planTypeProperty.put("description", "计划类型：muscle_building（增肌）/ fat_loss（减脂）/ strength（力量）");
        properties.put("planType", planTypeProperty);
        
        Map<String, Object> weeklyDaysProperty = new HashMap<>();
        weeklyDaysProperty.put("type", "integer");
        weeklyDaysProperty.put("description", "每周训练天数");
        properties.put("weeklyDays", weeklyDaysProperty);
        
        schema.put("properties", properties);
        
        List<String> required = new ArrayList<>();
        required.add("planName");
        required.add("planType");
        required.add("weeklyDays");
        schema.put("required", required);
        
        return schema;
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
