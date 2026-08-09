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

    @Autowired
    private AuthorizedToolResolver authorizedToolResolver;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private static final String DOMAIN_OWNER = "nutrition";
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
            
            // 饮食记录由模型依据完整对话和受控草案契约抽取；
            // 不在 Agent 内用本地食物表或正则构造营养数据。
            if (isDeterministicDietRecording(task)) {
                logger.info("Taking LLM diet draft path");
                return executeWithToolLoop(task, userId, true);
            }
            
            // 3. 其他场景：工具调用循环（有策略）
            logger.info("Taking LLM loop path: nutrition analysis");
            return executeWithToolLoop(task, userId, false);
            
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
     * 由统一工具契约验证任务授权，避免在 Agent 内重复维护工具白名单。
     */
    private boolean validateToolPermissions(List<String> requestedTools) {
        try {
            authorizedToolResolver.validateAuthorization(requestedTools, List.of(DOMAIN_OWNER), true);
            return true;
        } catch (IllegalArgumentException e) {
            logger.warn("NutritionAgent received invalid tool authorization: {}", e.getMessage());
            return false;
        }
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

        if (containsDietQueryIntent(lower)) {
            return false;
        }

        // “吃了/喝了”只有在后面带实际食物时才是记录意图。
        boolean hasRecordIntent =
            lower.contains("记录") ||
            lower.contains("保存") ||
            lower.contains("添加") ||
            hasExplicitConsumptionWithFood(lower) ||
            lower.contains("早餐") ||
            lower.contains("午餐") ||
            lower.contains("晚餐") ||
            lower.contains("加餐");

        boolean hasDraftTool = task.getAllowedToolNames() != null &&
            task.getAllowedToolNames().contains(DRAFT_TOOL);

        return hasRecordIntent && hasDraftTool;
    }

    private boolean containsDietQueryIntent(String message) {
        boolean asksConsumedItems = message.contains("吃了什么") || message.contains("喝了什么") ||
            message.contains("吃的什么") || message.contains("喝的什么");
        boolean asksNutritionTotal = message.contains("多少热量") || message.contains("热量多少") ||
            message.contains("摄入多少") || message.contains("摄入了多少") || message.contains("吃了多少");
        return asksConsumedItems || asksNutritionTotal || message.contains("饮食情况") ||
            message.contains("饮食汇总") || message.contains("营养摄入") || message.contains("营养情况");
    }

    private boolean hasExplicitConsumptionWithFood(String message) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("(?:吃了|喝了)\\s*(.+)")
            .matcher(message);
        if (!matcher.find()) {
            return false;
        }
        String consumedContent = matcher.group(1)
            .replaceAll("[，,。！？!?；;]", "")
            .trim();
        return !consumedContent.isEmpty() && !consumedContent.matches(".*(?:什么|多少|几).*?");
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
                result.setStatus(AgentResultDto.ResultStatus.PARTIAL);
                result.getFindings().add("未能识别出可记录的食物和份量，请补充后重新生成饮食草案。");
                result.getConstraints().add("尚未创建饮食记录草案");
                result.setConfidence(0.0);
                return result;
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
            result.getFacts().add(Map.of("key", "用户请求", "value", task.getUserQuestion()));
            result.getFacts().add(Map.of("key", "日期", "value", draftDto.getDate()));
            result.getFacts().add(Map.of("key", "记录数", "value", draftDto.getRecords().size()));
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
            
            // 用户明确指定日期时优先解析；未指定时才默认今天。
            dto.setDate(resolveDietRecordDate(userQuestion));
            
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
            
            // 解析当前支持的常见食物；未知食物不应交给最终文本伪装成已保存。
            if (lower.contains("鸡蛋")) {
                records.add(createRecordItem(mealType, "鸡蛋", "2个", 140, 12.0f, 1.0f, 10.0f));
            }
            if (lower.contains("鸡胸肉") || lower.contains("鸡胸")) {
                records.add(createRecordItem(mealType, "鸡胸肉", extractAmount(userQuestion, "鸡胸肉", "100克"), 165, 31.0f, 0.0f, 3.6f));
            }
            if (lower.contains("鸡腿")) {
                records.add(createRecordItem(mealType, "鸡腿", extractAmount(userQuestion, "鸡腿", "1个"), 180, 25.0f, 0.0f, 8.0f));
            }
            if (lower.contains("牛奶")) {
                records.add(createRecordItem(mealType, "牛奶", extractAmount(userQuestion, "牛奶", "200ml"), 120, 6.0f, 10.0f, 6.0f));
            }
            if (lower.contains("米饭")) {
                records.add(createRecordItem(mealType, "米饭", extractAmount(userQuestion, "米饭", "100克"), 116, 2.6f, 25.9f, 0.3f));
            }
            if (lower.contains("燕麦")) {
                records.add(createRecordItem(mealType, "燕麦", extractAmount(userQuestion, "燕麦", "50克"), 190, 6.5f, 33.0f, 3.5f));
            }
            if (lower.contains("馒头")) {
                records.add(createRecordItem(mealType, "馒头", extractAmount(userQuestion, "馒头", "1个"), 223, 7.0f, 47.0f, 1.1f));
            }
            if (lower.contains("海鲜汤")) {
                records.add(createRecordItem(mealType, "海鲜汤", extractAmount(userQuestion, "海鲜汤", "1份"), 120, 12.0f, 6.0f, 4.0f));
            }
            
            // 对白名单之外的食物保留原始名称，避免未知食物被静默丢弃。
            appendGenericFoodRecords(userQuestion, mealType, records);
            
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

    private String resolveDietRecordDate(String userQuestion) {
        LocalDate today = LocalDate.now();
        String normalizedQuestion = userQuestion.replaceAll("\\s+", "");

        java.util.regex.Matcher explicitDate = java.util.regex.Pattern.compile(
            "(?<!\\d)(\\d{4})[-/.年](\\d{1,2})[-/.月](\\d{1,2})(?:日)?"
        ).matcher(normalizedQuestion);
        if (explicitDate.find()) {
            try {
                return LocalDate.of(
                    Integer.parseInt(explicitDate.group(1)),
                    Integer.parseInt(explicitDate.group(2)),
                    Integer.parseInt(explicitDate.group(3))
                ).format(dateFormatter);
            } catch (java.time.DateTimeException ignored) {
                // 无效的显式日期不应产生不可保存的草案，改用默认日期。
            }
        }

        if (normalizedQuestion.contains("前天")) {
            return today.minusDays(2).format(dateFormatter);
        }
        if (normalizedQuestion.contains("昨天")) {
            return today.minusDays(1).format(dateFormatter);
        }
        return today.format(dateFormatter);
    }
    
    private DietRecordDraftDto.RecordItem createRecordItem(
            String mealType, String foodName, String amount, int calories,
            float protein, float carbs, float fat) {
        DietRecordDraftDto.RecordItem item = new DietRecordDraftDto.RecordItem();
        item.setMealType(mealType);
        item.setFoodName(foodName);
        item.setAmount(amount);
        item.setCalories(calories);
        item.setProtein(protein);
        item.setCarbs(carbs);
        item.setFat(fat);
        return item;
    }

    private DietRecordDraftDto.RecordItem createRecordItem(
            String mealType, String foodName, String amount, int calories,
            float protein, float carbs, float fat, boolean estimated) {
        DietRecordDraftDto.RecordItem item = createRecordItem(
            mealType, foodName, amount, calories, protein, carbs, fat
        );
        item.setEstimated(estimated);
        return item;
    }

    private String extractAmount(String userQuestion, String foodName, String fallback) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
            java.util.regex.Pattern.quote(foodName) + "\\s*(\\d+(?:\\.\\d+)?\\s*(?:克|g|毫升|ml|个|只|碗|杯|份|块|片|袋|盒|瓶|勺|斤|公斤|kg))",
            java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(userQuestion);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    /**
     * 保留白名单外的食物。未知项以待确认草案进入后续流程。
     */
    private void appendGenericFoodRecords(
            String userQuestion,
            String mealType,
            List<DietRecordDraftDto.RecordItem> records) {
        Set<String> knownFoods = new HashSet<>();
        for (DietRecordDraftDto.RecordItem record : records) {
            knownFoods.add(record.getFoodName());
        }

        String[] segments = userQuestion.split("[，,、；;和及与+]");
        for (String rawSegment : segments) {
            String segment = rawSegment
                .replaceAll("^(补充信息[：:]|帮我|请|我要|想要|给我|我想|我刚|我|记录|保存|添加|吃了|喝了)+", "")
                .replaceAll("(的饮食记录|饮食记录|记录|保存|添加)$", "")
                .trim();
            if (segment.isEmpty()) {
                continue;
            }

            java.util.regex.Matcher amountBeforeFood = java.util.regex.Pattern.compile(
                "^((?:\\d+(?:\\.\\d+)?|[一二两三四五六七八九十百半]+)\\s*(?:克|g|毫升|ml|个|只|碗|杯|份|块|片|袋|盒|瓶|勺|锅|斤|公斤|kg))\\s*([\\p{IsHan}A-Za-z][\\p{IsHan}A-Za-z0-9·]{1,19})$",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(segment);
            java.util.regex.Matcher foodBeforeAmount = java.util.regex.Pattern.compile(
                "^([\\p{IsHan}A-Za-z][\\p{IsHan}A-Za-z0-9·]{1,19})\\s*((?:\\d+(?:\\.\\d+)?|[一二两三四五六七八九十百半]+)\\s*(?:克|g|毫升|ml|个|只|碗|杯|份|块|片|袋|盒|瓶|勺|锅|斤|公斤|kg))$",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(segment);

            String foodName;
            String amount;
            if (amountBeforeFood.matches()) {
                amount = amountBeforeFood.group(1).trim();
                foodName = amountBeforeFood.group(2).trim();
            } else if (foodBeforeAmount.matches()) {
                foodName = foodBeforeAmount.group(1).trim();
                amount = foodBeforeAmount.group(2).trim();
            } else {
                foodName = segment.replaceAll("^[一二两三四五六七八九十百半]+", "").trim();
                amount = "1份（待确认）";
                if (!foodName.matches("[\\p{IsHan}A-Za-z][\\p{IsHan}A-Za-z0-9·]{1,19}")) {
                    continue;
                }
            }

            if (knownFoods.contains(foodName) || containsNonFoodPhrase(foodName) ||
                isPreparationOrMealDetail(foodName)) {
                continue;
            }
            records.add(createRecordItem(mealType, foodName, amount, 0, 0.0f, 0.0f, 0.0f, true));
            knownFoods.add(foodName);
            logger.info("Unknown food requires LLM estimation: {} {}", foodName, amount);
        }
    }

    private boolean containsNonFoodPhrase(String value) {
        return value.contains("早餐") || value.contains("午餐") || value.contains("晚餐") ||
            value.contains("加餐") || value.contains("帮我") || value.contains("请问");
    }

    /**
     * 烹饪方式和餐次是未知食物估算的上下文，不能被伪造为独立食物记录。
     */
    private boolean isPreparationOrMealDetail(String value) {
        return value.equals("油炸") || value.equals("煎") || value.equals("炒") ||
            value.equals("蒸") || value.equals("煮") || value.equals("烤") ||
            value.equals("炖") || value.equals("焖") || value.equals("凉拌") ||
            value.equals("清炒") || value.equals("红烧") || value.equals("早餐") ||
            value.equals("午餐") || value.equals("晚餐") || value.equals("加餐");
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
    private AgentResultDto executeWithToolLoop(AgentTaskDto task, Long userId, boolean dietDraftRequired) {
        logger.info("Tool loop execution for task: {}", task.getTaskId());
        
        AgentResultDto result = initializeResult(task);
        
        try {
            // 构建饮食领域的系统提示
            String systemPrompt = buildNutritionAgentSystemPrompt(task, dietDraftRequired);
            
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
                
                // 记录饮食时强制模型调用草案工具，未知食物由模型判断营养值。
                LlmClient.LlmResponse llmResponse = dietDraftRequired
                    ? llmClient.chat(messages, allowedTools, DRAFT_TOOL)
                    : llmClient.chat(messages, allowedTools);
                
                // 处理工具调用
                if (llmResponse.hasToolCalls()) {
                    if (!"tool_calls".equals(llmResponse.getFinishReason())) {
                        throw new RuntimeException("LLM工具调用缺少tool_calls终止原因");
                    }
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
                                logger.warn("Failed to parse diet draft result", e);
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
                
                if (!"stop".equals(llmResponse.getFinishReason())) {
                    throw new RuntimeException("LLM未正常完成回复，终止原因: " + llmResponse.getFinishReason());
                }
                if (llmResponse.getContent() != null && !llmResponse.getContent().isBlank()) {
                    finalResponse = llmResponse.getContent();
                    break;
                }
                
                // 异常情况：既无工具调用也无内容
                logger.error("NutritionAgent: invalid LLM response at iteration {}", iteration);
                throw new RuntimeException("LLM返回了无效响应");
            }
            
            // 与 F 一致：明确记录请求只能在有效草案产生后进入确认状态。
            if (dietDraftRequired && !draftGenerated) {
                result.setStatus(AgentResultDto.ResultStatus.PARTIAL);
                result.getFindings().add("未能生成饮食记录草案，请补充食物和份量后重试。");
                result.getConstraints().add("尚未创建饮食记录草案");
                result.setConfidence(0.2);
                return result;
            }

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
                logger.warn("NutritionAgent attempted unauthorized tool: {}", toolName);
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
            
            logger.info("NutritionAgent executed tool: {}", toolName);
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
    private String buildNutritionAgentSystemPrompt(AgentTaskDto task, boolean dietDraftRequired) {
        String dietRecordingRule = dietDraftRequired ? """
            当前请求是饮食记录。你必须调用create_diet_record_draft，不能只返回普通文本。
            必须从用户原话提取全部食物和份量，包括不在任何固定名单中的食物。
            未知食物由你根据名称、烹饪方式和份量估算calories、protein、carbs、fat，并把is_estimated设为true；
            无法可靠估算时不要调用草案工具，改为向用户询问食材、烹饪方式或重量。
            已知食物的is_estimated设为false。
            不得把未知食物丢弃，不得伪造“已保存”。
            """ : "";
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
            %s
            """, 
            task.getObjective() != null ? task.getObjective() : "饮食领域分析",
            task.getAllowedToolNames() != null ? String.join(", ", task.getAllowedToolNames()) : "无",
            dietRecordingRule
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
