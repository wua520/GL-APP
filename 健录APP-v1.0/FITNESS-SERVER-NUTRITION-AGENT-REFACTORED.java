package com.fitness.server.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.server.agent.dto.AgentResultDto;
import com.fitness.server.agent.dto.AgentTaskDto;
import com.fitness.server.agent.dto.DietRecordDraftDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Nutrition Agent - 饮食领域专家（重构版）
 * 
 * Phase F1-A 架构：
 * - 职责清晰：只处理饮食领域任务
 * - 确定性路径：明确的饮食记录意图直接调用工具
 * - 结构化契约：AgentTask → AgentResult
 * - 最小工具集：只获得授权的饮食工具
 * 
 * 不做：
 * - 不处理训练、进度等其他领域
 * - 不自由决定工具权限
 * - 不直接写入数据库
 * - 不编造数据
 */
@Component
public class NutritionAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(NutritionAgent.class);
    
    @Autowired
    private AgentToolExecutorV2 toolExecutor;
    
    @Autowired
    private LlmClient llmClient;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Nutrition Agent 的最小工具集（READ工具）
    private static final List<String> READ_TOOLS = List.of(
        "get_diet_summary",
        "get_daily_nutrition_progress",
        "get_today_diet_summary",
        "get_user_fitness_profile"
    );
    
    // 唯一的草案工具
    private static final String DRAFT_TOOL = "create_diet_record_draft";
    
    /**
     * 执行饮食领域任务
     * 
     * @param task 任务描述（由Supervisor分配）
     * @param userId 用户ID（由Supervisor从JWT注入，不可信模型）
     * @return 结构化结果
     */
    public AgentResultDto execute(AgentTaskDto task, Long userId) {
        logger.info("NutritionAgent executing task: {} for user: {}", task.getTaskId(), userId);
        
        AgentResultDto result = initializeResult(task);
        
        try {
            // 1. 验证工具权限（防御性检查）
            if (!validateToolPermissions(task.getAllowedToolNames())) {
                return createFailedResult(task, "Unauthorized tools detected");
            }
            
            // 2. 检测确定性路径：饮食记录创建
            if (isDeterministicDietRecording(task)) {
                logger.info("Taking deterministic path: diet recording");
                return executeDeterministicDietRecording(task, userId);
            }
            
            // 3. 其他场景：工具调用循环（有策略）
            logger.info("Taking LLM loop path: nutrition analysis");
            return executeWithToolLoop(task, userId);
            
        } catch (Exception e) {
            logger.error("NutritionAgent execution failed", e);
            return createFailedResult(task, "Execution error: " + e.getMessage());
        }
    }
    
    /**
     * 初始化结果对象
     */
    private AgentResultDto initializeResult(AgentTaskDto task) {
        AgentResultDto result = new AgentResultDto();
        result.setTaskId(task.getTaskId());
        result.setAgentName("NutritionAgent");
        result.setFacts(new ArrayList<>());
        result.setFindings(new ArrayList<>());
        result.setConstraints(new ArrayList<>());
        result.setEvidenceRefs(new ArrayList<>());
        return result;
    }
    
    /**
     * 验证工具权限：只能使用授权的饮食工具
     */
    private boolean validateToolPermissions(List<String> requestedTools) {
        if (requestedTools == null || requestedTools.isEmpty()) {
            return true; // 无工具请求
        }
        
        List<String> allAllowedTools = new ArrayList<>(READ_TOOLS);
        allAllowedTools.add(DRAFT_TOOL);
        
        for (String tool : requestedTools) {
            if (!allAllowedTools.contains(tool)) {
                logger.warn("NutritionAgent: unauthorized tool requested: {}", tool);
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检测确定性路径：饮食记录创建意图
     * 
     * 满足条件：
     * 1. 用户问题包含记录意图关键词
     * 2. 问题中包含食物信息
     * 3. 被授权使用草案工具
     */
    private boolean isDeterministicDietRecording(AgentTaskDto task) {
        String lower = task.getUserQuestion().toLowerCase();
        
        // 记录意图检测
        boolean hasRecordIntent = 
            lower.contains("记录") || 
            lower.contains("吃了") || 
            lower.contains("早餐") ||
            lower.contains("午餐") ||
            lower.contains("晚餐") ||
            lower.contains("加餐");
        
        // 食物信息检测（至少包含常见食物关键词）
        boolean hasFoodInfo = 
            lower.matches(".*(鸡蛋|牛奶|鸡胸|米饭|面包|燕麦|香蕉|苹果|蔬菜|肉|鱼|虾).*") ||
            lower.matches(".*\\d+克.*") ||  // 包含克数
            lower.matches(".*\\d+g.*");     // 包含g
        
        // 工具授权检测
        boolean hasDraftTool = task.getAllowedToolNames() != null && 
            task.getAllowedToolNames().contains(DRAFT_TOOL);
        
        return hasRecordIntent && hasFoodInfo && hasDraftTool;
    }
    
    /**
     * 确定性路径：饮食记录创建
     * 
     * 从用户问题提取食物信息，直接调用草案工具生成结果
     * 保证100%生成草案（如果信息提取成功且工具执行成功）
     */
    private AgentResultDto executeDeterministicDietRecording(AgentTaskDto task, Long userId) {
        logger.info("Deterministic diet recording for task: {}", task.getTaskId());
        
        AgentResultDto result = initializeResult(task);
        
        try {
            // 从用户问题提取饮食记录参数
            DietRecordDraftDto draftDto = extractDietRecordArguments(task.getUserQuestion());
            
            if (draftDto == null || draftDto.getRecords() == null || draftDto.getRecords().isEmpty()) {
                // 信息提取失败，降级到LLM循环
                logger.warn("Failed to extract diet info, falling back to LLM loop");
                return executeWithToolLoop(task, userId);
            }
            
            // 直接调用草案工具
            String draftJson = objectMapper.writeValueAsString(draftDto);
            String toolResult = toolExecutor.executeTool(
                userId,
                task.getAllowedToolNames(),
                DRAFT_TOOL,
                draftJson
            );
            
            // 解析工具返回的normalized_draft_json
            JsonNode resultNode = objectMapper.readTree(toolResult);
            String normalizedDraft = resultNode.has("normalized_draft_json") ?
                resultNode.get("normalized_draft_json").asText() : null;
            
            if (normalizedDraft == null || normalizedDraft.isEmpty()) {
                // 草案工具未返回有效结果
                result.setStatus(AgentResultDto.ResultStatus.PARTIAL);
                result.getFindings().add("饮食记录草案生成失败，工具未返回有效数据");
                result.setConfidence(0.3);
                logger.warn("Draft tool returned no valid normalized_draft_json");
                return result;
            }
            
            // 成功生成草案
            result.setStatus(AgentResultDto.ResultStatus.SUCCESS);
            result.getFacts().add("用户请求：" + task.getUserQuestion());
            result.getFacts().add("日期：" + draftDto.getDate());
            result.getFacts().add("记录数：" + draftDto.getRecords().size());
            result.getFindings().add("已为你准备好饮食记录草案，请确认后保存。");
            result.setDraftCandidate(normalizedDraft);
            result.setConfidence(0.9);
            result.getEvidenceRefs().add("tool:create_diet_record_draft");
            
            logger.info("Diet draft created successfully for task: {}", task.getTaskId());
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
            logger.error("Deterministic diet recording failed", e);
            result.setStatus(AgentResultDto.ResultStatus.FAILED);
            result.setFailureReason("技术错误: " + e.getMessage());
            result.setConfidence(0.0);
            return result;
        }
    }
    
    /**
     * 从用户问题提取饮食记录参数
     * 
     * 简化实现：基于关键词匹配和正则表达式
     * 生产环境应使用更健壮的NLP或LLM提取
     */
    private DietRecordDraftDto extractDietRecordArguments(String userQuestion) {
        try {
            DietRecordDraftDto dto = new DietRecordDraftDto();
            List<DietRecordDraftDto.RecordItem> records = new ArrayList<>();
            
            // 提取日期（默认今天）
            String date = LocalDate.now().format(dateFormatter);
            dto.setDate(date);
            
            String lower = userQuestion.toLowerCase();
            
            // 提取餐次
            String mealType = "加餐"; // 默认
            if (lower.contains("早餐")) {
                mealType = "早餐";
            } else if (lower.contains("午餐") || lower.contains("中餐")) {
                mealType = "午餐";
            } else if (lower.contains("晚餐")) {
                mealType = "晚餐";
            }
            
            // 简化提取：假设用户格式为 "记录早餐：鸡蛋2个、牛奶200ml"
            // 实际需要更复杂的解析逻辑
            
            // 尝试匹配常见食物和量
            Map<String, String> foodPatterns = new HashMap<>();
            foodPatterns.put("鸡蛋", "(\\d+)个");
            foodPatterns.put("鸡胸肉", "(\\d+)克");
            foodPatterns.put("牛奶", "(\\d+)ml");
            foodPatterns.put("燕麦", "(\\d+)克");
            foodPatterns.put("米饭", "(\\d+)克");
            
            // 示例：简单提取一个食物
            // 实际应该解析多个食物并估算营养值
            if (lower.contains("鸡蛋")) {
                DietRecordDraftDto.RecordItem item = new DietRecordDraftDto.RecordItem();
                item.setMealType(mealType);
                item.setFoodName("鸡蛋");
                item.setAmount("2个"); // 简化假设
                item.setCalories(140); // 估算值
                item.setProtein(12.0f);
                item.setCarbs(1.0f);
                item.setFat(10.0f);
                records.add(item);
            } else if (lower.contains("牛奶")) {
                DietRecordDraftDto.RecordItem item = new DietRecordDraftDto.RecordItem();
                item.setMealType(mealType);
                item.setFoodName("牛奶");
                item.setAmount("200ml");
                item.setCalories(120);
                item.setProtein(6.0f);
                item.setCarbs(10.0f);
                item.setFat(6.0f);
                records.add(item);
            }
            
            if (records.isEmpty()) {
                logger.warn("No food extracted from: {}", userQuestion);
                return null;
            }
            
            dto.setRecords(records);
            return dto;
            
        } catch (Exception e) {
            logger.error("Failed to extract diet arguments", e);
            return null;
        }
    }
    
    /**
     * 工具调用循环路径：营养分析和查询场景
     * 
     * 使用LLM工具调用能力，但有明确策略：
     * - 最多5轮
     * - 只能调用授权工具
     * - 工具结果验证
     * - 超时检测
     * - 草案生成后立即结束
     */
    private AgentResultDto executeWithToolLoop(AgentTaskDto task, Long userId) {
        logger.info("Tool loop execution for task: {}", task.getTaskId());
        
        AgentResultDto result = initializeResult(task);
        
        try {
            // 构建饮食领域的系统提示
            String systemPrompt = buildNutritionAgentSystemPrompt(task);
            
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
            boolean draftGenerated = false;
            
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
                
                logger.debug("NutritionAgent iteration {}/{}", iteration, maxIterations);
                
                // 调用LLM
                LlmClient.LlmResponse llmResponse = llmClient.chat(messages, allowedTools);
                
                // 处理工具调用
                if (llmResponse.hasToolCalls()) {
                    logger.info("NutritionAgent: {} tool calls in iteration {}", 
                        llmResponse.getToolCalls().size(), iteration);
                    
                    // 添加assistant消息（包含tool_calls）
                    messages.add(new LlmClient.Message("assistant", 
                        llmResponse.getContent(), llmResponse.getToolCalls()));
                    
                    // 执行每个工具调用
                    for (LlmClient.ToolCall toolCall : llmResponse.getToolCalls()) {
                        String toolResult = executeToolCall(
                            toolCall, userId, task.getAllowedToolNames(), toolsUsed);
                        
                        // 如果是草案工具且成功，立即结束
                        if (DRAFT_TOOL.equals(toolCall.getName())) {
                            try {
                                JsonNode resultNode = objectMapper.readTree(toolResult);
                                if (resultNode.has("normalized_draft_json")) {
                                    String draftJson = resultNode.get("normalized_draft_json").asText();
                                    result.setDraftCandidate(draftJson);
                                    draftGenerated = true;
                                    finalResponse = "已为你准备好饮食记录草案，请确认后保存。";
                                    logger.info("Diet draft generated, terminating loop");
                                }
                            } catch (Exception e) {
                                logger.warn("Failed to parse draft result", e);
                            }
                        }
                        
                        // 添加工具结果到消息
                        messages.add(new LlmClient.Message("tool", toolResult, toolCall.getId()));
                    }
                    
                    // 如果草案已生成，跳出循环
                    if (draftGenerated) {
                        break;
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
                logger.error("NutritionAgent: invalid LLM response at iteration {}", iteration);
                throw new RuntimeException("LLM返回了无效响应");
            }
            
            // 超出最大迭代次数
            if (finalResponse == null && !draftGenerated) {
                logger.warn("NutritionAgent reached max iterations without final response");
                result.setStatus(AgentResultDto.ResultStatus.PARTIAL);
                result.getFindings().add("分析达到最大轮次限制，请尝试简化问题");
                result.setConfidence(0.3);
                return result;
            }
            
            // 成功完成
            result.setStatus(AgentResultDto.ResultStatus.SUCCESS);
            if (finalResponse != null) {
                result.getFindings().add(finalResponse);
            }
            result.setConfidence(0.8);
            
            // 记录使用的工具作为证据
            for (String toolName : toolsUsed) {
                result.getEvidenceRefs().add("tool:" + toolName);
            }
            
            // 提取约束信息
            if (finalResponse != null) {
                extractConstraintsFromResponse(finalResponse, result);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Tool loop execution failed", e);
            result.setStatus(AgentResultDto.ResultStatus.FAILED);
            result.setFailureReason("饮食分析失败: " + e.getMessage());
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
                logger.warn("NutritionAgent attempted unauthorized tool: {}", toolName);
                return error;
            }
            
            // 执行工具
            String result = toolExecutor.executeTool(
                userId,
                allowedToolNames,
                toolName,
                toolCall.getArguments()
            );
            
            logger.info("NutritionAgent executed tool: {}", toolName);
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
            result.getConstraints().add("包含不建议的食物或饮食方式");
        }
        
        if (lower.contains("超标") || lower.contains("过量")) {
            result.getConstraints().add("营养摄入可能超标");
        }
        
        if (lower.contains("数据不足") || lower.contains("无法判断")) {
            result.getConstraints().add("数据不足，分析可能不准确");
        }
        
        if (lower.contains("咨询营养师") || lower.contains("咨询医生") || 
            lower.contains("专业指导") || lower.contains("就医")) {
            result.getConstraints().add("需要专业营养师或医疗指导");
        }
    }
    
    /**
     * 构建Nutrition Agent专用系统提示
     */
    private String buildNutritionAgentSystemPrompt(AgentTaskDto task) {
        return String.format("""
            你是健录App的饮食领域专家Agent。
            
            你的职责范围：
            - 分析用户的饮食数据和营养摄入
            - 提供饮食和营养建议
            - 回答饮食相关问题
            
            你的严格限制：
            - 只能使用授权的饮食工具
            - 不能处理训练、健身、身体数据趋势等其他领域问题
            - 不能提供医疗诊断
            - 遇到疾病、药物、极端节食问题，必须建议咨询医生或营养师
            - 不能编造数据，只能基于工具返回的真实数据
            
            当前任务目标：%s
            授权工具：%s
            
            工作原则：
            1. 按需调用工具（不要一次性获取所有数据）
            2. 基于数据给出具体建议
            3. 数据不足时明确说明
            4. 回复简洁专业
            5. 如果调用create_diet_record_draft工具成功，直接回复"已为你准备好饮食记录草案，请确认后保存"
            """, 
            task.getObjective() != null ? task.getObjective() : "饮食领域分析",
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
            case "get_diet_summary" -> new LlmClient.Tool(
                "get_diet_summary",
                "获取用户指定日期范围的饮食摘要",
                createDateRangeParameterSchema(),
                "READ"
            );
            case "get_daily_nutrition_progress" -> new LlmClient.Tool(
                "get_daily_nutrition_progress",
                "获取用户每日营养目标完成进度",
                createDateParameterSchema(),
                "READ"
            );
            case "get_today_diet_summary" -> new LlmClient.Tool(
                "get_today_diet_summary",
                "获取用户今天的饮食摘要",
                createEmptyParametersSchema(),
                "READ"
            );
            case "get_user_fitness_profile" -> new LlmClient.Tool(
                "get_user_fitness_profile",
                "获取用户的健身档案（包含营养目标）",
                createEmptyParametersSchema(),
                "READ"
            );
            case "create_diet_record_draft" -> new LlmClient.Tool(
                "create_diet_record_draft",
                "创建饮食记录草案（需要用户确认）",
                createDietRecordDraftParameterSchema(),
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
    
    private Map<String, Object> createDateRangeParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> daysProperty = new HashMap<>();
        daysProperty.put("type", "integer");
        daysProperty.put("description", "要获取最近多少天的饮食摘要");
        daysProperty.put("default", 7);
        properties.put("rangeDays", daysProperty);
        
        schema.put("properties", properties);
        schema.put("required", new ArrayList<>());
        return schema;
    }
    
    private Map<String, Object> createDateParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> dateProperty = new HashMap<>();
        dateProperty.put("type", "string");
        dateProperty.put("description", "查询日期 (yyyy-MM-dd格式)");
        properties.put("date", dateProperty);
        
        schema.put("properties", properties);
        schema.put("required", new ArrayList<>());
        return schema;
    }
    
    private Map<String, Object> createDietRecordDraftParameterSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> dateProperty = new HashMap<>();
        dateProperty.put("type", "string");
        dateProperty.put("description", "日期（yyyy-MM-dd格式）");
        properties.put("date", dateProperty);
        
        Map<String, Object> recordsProperty = new HashMap<>();
        recordsProperty.put("type", "array");
        recordsProperty.put("description", "饮食记录列表");
        properties.put("records", recordsProperty);
        
        schema.put("properties", properties);
        
        List<String> required = new ArrayList<>();
        required.add("date");
        required.add("records");
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
