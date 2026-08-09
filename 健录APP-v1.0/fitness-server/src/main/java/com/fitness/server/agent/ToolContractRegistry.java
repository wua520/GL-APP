package com.fitness.server.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * 工具契约注册表 - Phase G 唯一事实源
 * 
 * 替代原 AgentToolRegistry，解决Schema四套事实源问题
 * 
 * 所有工具的定义、参数、校验、执行绑定都从这里获取
 * 禁止在其他地方重复定义工具Schema
 */
@Component
public class ToolContractRegistry {
    
    private final LinkedHashMap<String, ToolContract> contracts = new LinkedHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ToolContractRegistry() {
        registerAllTools();
    }
    
    /**
     * 获取工具契约
     */
    public ToolContract getContract(String toolName) {
        return contracts.get(toolName);
    }
    
    /**
     * 获取所有工具契约
     */
    public List<ToolContract> getAllContracts() {
        return new ArrayList<>(contracts.values());
    }
    
    /**
     * 检查工具是否已注册
     */
    public boolean isRegistered(String toolName) {
        return contracts.containsKey(toolName);
    }
    
    /**
     * 按领域获取工具
     */
    public List<ToolContract> getContractsByDomain(String domain) {
        return contracts.values().stream()
            .filter(c -> domain.equals(c.getDomainOwner()))
            .toList();
    }
    
    public List<String> getToolNamesByCategory(ToolContract.ToolCategory category) {
        return contracts.values().stream()
            .filter(contract -> contract.getCategory() == category)
            .map(ToolContract::getName)
            .toList();
    }

    /**
     * Phase G Batch 1: 注册所有13个工具的契约定义
     * 这是唯一的工具事实源
     */
    private void registerAllTools() {
        // Training Domain - 7 tools
        registerTrainingTools();
        
        // Nutrition Domain - 3 tools
        registerNutritionTools();
        
        // Progress Domain - 3 tools
        registerProgressTools();
    }
    
    /**
     * 注册训练领域工具
     */
    private void registerTrainingTools() {
        // 1. get_training_summary
        register(new ToolContract.Builder()
            .name("get_training_summary")
            .description("获取用户的训练摘要统计")
            .category(ToolContract.ToolCategory.READ)
            .domainOwner("training")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                    "rangeDays", Map.of(
                        "type", "integer",
                        "description", "查询最近N天的训练，默认7天",
                        "minimum", 1,
                        "maximum", 90,
                        "default", 7
                    )
                ),
                "required", Collections.emptyList()
            ))
            .validator(argsJson -> validateIntegerParam(argsJson, "rangeDays", 7, 1, 90))
            .executorBinding(ToolContract.ExecutorBinding.TRAINING_SUMMARY)
            .build()
        );
        
        // 2. get_recent_workouts - 按最近天数查询
        register(new ToolContract.Builder()
            .name("get_recent_workouts")
            .description("获取用户最近N天的训练记录详情")
            .category(ToolContract.ToolCategory.READ)
            .domainOwner("training")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                    "rangeDays", Map.of(
                        "type", "integer",
                        "description", "查询最近训练记录的天数，默认7天",
                        "minimum", 1,
                        "maximum", 90,
                        "default", 7
                    )
                ),
                "required", Collections.emptyList()
            ))
            .validator(argsJson -> validateIntegerParam(argsJson, "rangeDays", 7, 1, 90))
            .executorBinding(ToolContract.ExecutorBinding.RECENT_WORKOUTS)
            .build()
        );
        
        // 3. get_active_training_plan - 空参数工具
        register(new ToolContract.Builder()
            .name("get_active_training_plan")
            .description("查询当前用户服务器中的训练计划：返回最近更新的3条计划、全部置顶计划及去重后的计划详情。最近计划与置顶计划均按更新时间倒序；plan字段兼容返回最新置顶计划。")
            .category(ToolContract.ToolCategory.READ)
            .domainOwner("training")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Collections.emptyMap(),
                "required", Collections.emptyList()
            ))
            .validator(argsJson -> validateNoParams(argsJson))
            .executorBinding(ToolContract.ExecutorBinding.ACTIVE_TRAINING_PLAN)
            .build()
        );
        
        // 4. get_training_schedule
        register(new ToolContract.Builder()
            .name("get_training_schedule")
            .description("""
                查询指定日期对应的训练计划安排。
                适用于用户询问"今天练什么"、"明天的训练安排"等场景。
                
                返回当天计划的训练内容，包括动作、组数、次数、休息时间等。
                如果没有计划或当天为休息日，hasWorkout为false。
                """)
            .category(ToolContract.ToolCategory.READ)
            .domainOwner("training")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                    "date", Map.of(
                        "type", "string",
                        "description", "查询日期，格式：yyyy-MM-dd。不提供则使用今天",
                        "pattern", "^\\d{4}-\\d{2}-\\d{2}$"
                    )
                ),
                "required", Collections.emptyList()
            ))
            .validator(argsJson -> validateOptionalDate(argsJson, true, 90, 30))
            .executorBinding(ToolContract.ExecutorBinding.TRAINING_SCHEDULE)
            .build()
        );
        
        // 5. get_training_progress
        register(new ToolContract.Builder()
            .name("get_training_progress")
            .description("""
                统计指定时间范围内训练计划的执行情况。
                
                返回计划的训练天数、实际完成天数、完成率、连续训练天数等。
                所有统计结果由服务端计算，模型只能解释结果，不能自行重新计算。
                
                如果用户没有活跃计划，hasActivePlan为false。
                """)
            .category(ToolContract.ToolCategory.READ)
            .domainOwner("training")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                    "rangeDays", Map.of(
                        "type", "integer",
                        "description", "统计最近N天的训练进度，默认30天",
                        "minimum", 7,
                        "maximum", 90,
                        "default", 30
                    )
                ),
                "required", Collections.emptyList()
            ))
            .validator(argsJson -> validateIntegerParam(argsJson, "rangeDays", 30, 7, 90))
            .executorBinding(ToolContract.ExecutorBinding.TRAINING_PROGRESS)
            .build()
        );
        
        // 6. get_recovery_status - 空参数工具
        register(new ToolContract.Builder()
            .name("get_recovery_status")
            .description("根据最近训练的部位和间隔判断各肌群的恢复状态")
            .category(ToolContract.ToolCategory.READ)
            .domainOwner("training")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Collections.emptyMap(),
                "required", Collections.emptyList()
            ))
            .validator(argsJson -> validateNoParams(argsJson))
            .executorBinding(ToolContract.ExecutorBinding.RECOVERY_STATUS)
            .build()
        );
        
        // 7. create_training_plan_draft - 草案工具（需要用户确认）
        register(new ToolContract.Builder()
            .name("create_training_plan_draft")
            .description("""
                创建训练计划草案，生成结构化的训练计划供用户确认。
                必须先了解用户的目标、经验水平、可训练天数、可用器械和身体限制。
                
                **严格的JSON输出格式示例**：
                {
                  "title": "4周增肌计划",
                  "description": "针对中级训练者的4天分化训练",
                  "goal": "增肌",
                  "experience": "中级",
                  "targetMuscles": "胸,背,腿,肩",
                  "trainingDays": 4,
                  "trainingDuration": "60-75分钟",
                  "equipment": "哑铃,杠铃",
                  "days": [
                    {
                      "name": "第1天",
                      "focus": "胸部+三头",
                      "exercises": [
                        {
                          "name": "杠铃卧推",
                          "sets": 4,
                          "reps": "8-12次",
                          "restTime": "90秒",
                          "notes": "注意控制节奏"
                        }
                      ]
                    }
                  ]
                }
                
                **格式要求**：
                - reps必须带单位"次"（如"8-12次"）
                - restTime必须带时间单位"秒"或"分钟"（如"90秒"、"2分钟"）
                - notes控制在15字以内
                - 每天最多5个动作
                """)
            .category(ToolContract.ToolCategory.DRAFT)
            .domainOwner("training")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                    "title", Map.of(
                        "type", "string",
                        "description", "计划标题"
                    ),
                    "description", Map.of(
                        "type", "string",
                        "description", "计划描述（不超过80字）"
                    ),
                    "goal", Map.of(
                        "type", "string",
                        "description", "训练目标（增肌/减脂/塑形/力量）"
                    ),
                    "experience", Map.of(
                        "type", "string",
                        "description", "经验水平（新手/中级/高级）"
                    ),
                    "targetMuscles", Map.of(
                        "type", "string",
                        "description", "目标肌群"
                    ),
                    "trainingDays", Map.of(
                        "type", "integer",
                        "description", "每周训练天数",
                        "minimum", 1,
                        "maximum", 7
                    ),
                    "trainingDuration", Map.of(
                        "type", "string",
                        "description", "每次训练时长"
                    ),
                    "equipment", Map.of(
                        "type", "string",
                        "description", "可用器械"
                    ),
                    "days", Map.of(
                        "type", "array",
                        "description", "每天的训练安排",
                        "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "name", Map.of("type", "string", "description", "第N天"),
                                "focus", Map.of("type", "string", "description", "该天训练重点"),
                                "exercises", Map.of(
                                    "type", "array",
                                    "description", "动作列表（每天最多5个）",
                                    "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                            "name", Map.of("type", "string", "description", "动作名称"),
                                            "sets", Map.of("type", "integer", "description", "组数"),
                                            "reps", Map.of("type", "string", "description", "次数，必须带'次'单位，如'8-12次'"),
                                            "restTime", Map.of("type", "string", "description", "组间休息，必须带时间单位，如'90秒'"),
                                            "notes", Map.of("type", "string", "description", "动作要点（不超过15字）")
                                        ),
                                        "required", List.of("name", "sets", "reps", "restTime")
                                    )
                                )
                            ),
                            "required", List.of("name", "focus", "exercises")
                        )
                    )
                ),
                "required", List.of("title", "goal", "experience", "trainingDays", "days")
            ))
            .validator(argsJson -> validateTrainingPlanDraft(argsJson))
            .executorBinding(ToolContract.ExecutorBinding.CREATE_TRAINING_PLAN_DRAFT)
            .build()
        );
    }
    
    /**
     * 注册饮食领域工具
     */
    private void registerNutritionTools() {
        // 8. get_today_diet_summary - 空参数工具
        register(new ToolContract.Builder()
            .name("get_today_diet_summary")
            .description("获取用户今天的饮食摘要（热量、蛋白质、碳水、脂肪）")
            .category(ToolContract.ToolCategory.READ)
            .domainOwner("nutrition")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Collections.emptyMap(),
                "required", Collections.emptyList()
            ))
            .validator(argsJson -> validateNoParams(argsJson))
            .executorBinding(ToolContract.ExecutorBinding.TODAY_DIET_SUMMARY)
            .build()
        );
        
        // 9. get_diet_summary - 修复默认值冲突：统一为1天
        register(new ToolContract.Builder()
            .name("get_diet_summary")
            .description("""
                查询指定日期或日期范围内的饮食记录摘要。
                适用于用户询问"昨天吃了什么"、"最近一周的饮食"等场景。
                
                返回每天的总热量、蛋白质、碳水、脂肪以及详细记录。
                只返回已保存的记录，没有数据时返回空结果。
                """)
            .category(ToolContract.ToolCategory.READ)
            .domainOwner("nutrition")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                    "date", Map.of(
                        "type", "string",
                        "description", "起始日期，格式：yyyy-MM-dd。不提供则使用今天",
                        "pattern", "^\\d{4}-\\d{2}-\\d{2}$"
                    ),
                    "rangeDays", Map.of(
                        "type", "integer",
                        "description", "查询天数范围，默认1天（仅查询指定日期），最大90天",
                        "minimum", 1,
                        "maximum", 90,
                        "default", 1
                    )
                ),
                "required", Collections.emptyList()
            ))
            .validator(argsJson -> validateDietSummary(argsJson))
            .executorBinding(ToolContract.ExecutorBinding.DIET_SUMMARY)
            .build()
        );
        
        // 10. create_diet_record_draft - 草案工具
        register(new ToolContract.Builder()
            .name("create_diet_record_draft")
            .description("""
                【必须调用】当用户表达要记录饮食时立即调用此工具。
                
                **必须调用的场景**（不要询问，直接调用）：
                - 用户说"帮我记录XXX"
                - 用户说"我吃了XXX"  
                - 用户说"添加XXX到饮食记录"
                - 用户描述了具体吃的食物
                
                **不要**：
                - 不要回复"已保存"、"已记录"等文字
                - 不要询问用户是否需要记录
                - 直接调用工具创建草案
                
                创建饮食记录草案，记录用户的饮食摄入供确认后保存。
                适用于用户告诉你今天吃了什么，或想要记录饮食的场景。
                
                **重要：日期使用规则**
                - 用户没有明确指定日期时，不要传date；服务端会将其规范化为当天日期
                - 用户说"今天"、"刚才吃的"等，也不要传date，由服务端使用当前日期
                - 仅当用户明确指定历史日期时才传date，且不得生成任意日期
                
                **严格的JSON输出格式示例**：
                {
                  "records": [
                    {
                      "meal_type": "早餐",
                      "food_name": "燕麦粥",
                      "calories": 150,
                      "protein": 5.0,
                      "carbs": 27.0,
                      "fat": 2.5,
                      "amount": "1碗"
                    },
                    {
                      "meal_type": "午餐",
                      "food_name": "鸡胸肉",
                      "calories": 165,
                      "protein": 31.0,
                      "carbs": 0.0,
                      "fat": 3.6,
                      "amount": "100g"
                    }
                  ]
                }
                
                **格式要求**：
                - date格式必须是"yyyy-MM-dd"，且必须是用户提到的日期或今天
                - meal_type只能是：早餐、午餐、晚餐、加餐
                - calories是整数（卡路里）
                - protein, carbs, fat是浮点数（单位：克）
                - amount描述份量，如"100g"、"1碗"、"1份"
                
                **营养估算**：
                - 如果用户没有提供详细营养数据，根据常见食物营养成分合理估算
                - 蛋白质：1g = 4卡路里
                - 碳水化合物：1g = 4卡路里
                - 脂肪：1g = 9卡路里
                """)
            .category(ToolContract.ToolCategory.DRAFT)
            .domainOwner("nutrition")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                    "date", Map.of(
                        "type", "string",
                        "description", "日期，格式：yyyy-MM-dd"
                    ),
                    "records", Map.of(
                        "type", "array",
                        "description", "饮食记录列表",
                        "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "meal_type", Map.of(
                                    "type", "string",
                                    "description", "餐次：早餐/午餐/晚餐/加餐",
                                    "enum", List.of("早餐", "午餐", "晚餐", "加餐")
                                ),
                                "food_name", Map.of(
                                    "type", "string",
                                    "description", "食物名称"
                                ),
                                "calories", Map.of(
                                    "type", "integer",
                                    "description", "热量（卡路里）"
                                ),
                                "protein", Map.of(
                                    "type", "number",
                                    "description", "蛋白质（克）"
                                ),
                                "carbs", Map.of(
                                    "type", "number",
                                    "description", "碳水化合物（克）"
                                ),
                                "fat", Map.of(
                                    "type", "number",
                                    "description", "脂肪（克）"
                                ),
                                "amount", Map.of(
                                    "type", "string",
                                    "description", "份量描述，如'100g'、'1碗'"
                                ),
                                "is_estimated", Map.of(
                                    "type", "boolean",
                                    "description", "营养值是否由模型估算；未知食物必须为true"
                                )
                            ),
                            "required", List.of("meal_type", "food_name", "calories", "protein", "carbs", "fat", "amount")
                        )
                    )
                ),
                "required", List.of("records")
            ))
            .validator(argsJson -> validateDietRecordDraft(argsJson))
            .executorBinding(ToolContract.ExecutorBinding.CREATE_DIET_RECORD_DRAFT)
            .build()
        );
    }
    
    /**
     * 注册进度领域工具
     */
    private void registerProgressTools() {
        // 11. get_user_fitness_profile - 空参数工具
        register(new ToolContract.Builder()
            .name("get_user_fitness_profile")
            .description("""
                获取用户的健身档案信息，用于生成个性化的训练或饮食建议。
                
                返回用户的目标、经验等级、身体数据、训练频率等信息。
                如果某些信息缺失，会明确标记为null，模型应主动向用户询问。
                
                **重要**：生成训练计划前，如果目标、经验或限制缺失，必须先询问用户。
                """)
            .category(ToolContract.ToolCategory.READ)
            .domainOwner("progress")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Collections.emptyMap(),
                "required", Collections.emptyList()
            ))
            .validator(argsJson -> validateNoParams(argsJson))
            .executorBinding(ToolContract.ExecutorBinding.USER_FITNESS_PROFILE)
            .build()
        );
        
        // 12. get_daily_nutrition_progress
        register(new ToolContract.Builder()
            .name("get_daily_nutrition_progress")
            .description("""
                查询指定日期的营养摄入与目标的对比情况。
                
                返回已摄入的营养、目标营养、差值和完成百分比。
                如果用户没有设置营养目标，hasTarget为false，此时只能描述摄入事实，
                不能声称用户"达标"或"不达标"。
                """)
            .category(ToolContract.ToolCategory.READ)
            .domainOwner("progress")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                    "date", Map.of(
                        "type", "string",
                        "description", "查询日期，格式：yyyy-MM-dd。不提供则使用今天",
                        "pattern", "^\\d{4}-\\d{2}-\\d{2}$"
                    )
                ),
                "required", Collections.emptyList()
            ))
            .validator(argsJson -> validateOptionalDate(argsJson, false, 365, 0))
            .executorBinding(ToolContract.ExecutorBinding.DAILY_NUTRITION_PROGRESS)
            .build()
        );
        
        // 13. get_body_trend
        register(new ToolContract.Builder()
            .name("get_body_trend")
            .description("获取用户的体重、体脂、肌肉量趋势")
            .category(ToolContract.ToolCategory.READ)
            .domainOwner("progress")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                    "rangeDays", Map.of(
                        "type", "integer",
                        "description", "查询最近N天的身体数据，默认30天",
                        "minimum", 1,
                        "maximum", 90,
                        "default", 30
                    )
                ),
                "required", Collections.emptyList()
            ))
            .validator(argsJson -> validateIntegerParam(argsJson, "rangeDays", 30, 1, 90))
            .executorBinding(ToolContract.ExecutorBinding.BODY_TREND)
            .build()
        );
    }

    private void register(ToolContract contract) {
        if (contracts.putIfAbsent(contract.getName(), contract) != null) {
            throw new IllegalStateException("工具契约重复注册: " + contract.getName());
        }
    }

    /**
     * 验证契约声明的执行目标与执行器实际提供的目标严格一一对应。
     */
    public void validateExecutorBindings(Set<ToolContract.ExecutorBinding> availableBindings) {
        if (availableBindings == null) {
            throw new IllegalStateException("执行绑定集合不能为空");
        }

        Set<ToolContract.ExecutorBinding> declaredBindings = EnumSet.noneOf(ToolContract.ExecutorBinding.class);
        for (ToolContract contract : contracts.values()) {
            if (!declaredBindings.add(contract.getExecutorBinding())) {
                throw new IllegalStateException("多个工具声明了同一执行绑定: " + contract.getExecutorBinding());
            }
        }
        if (!declaredBindings.equals(availableBindings)) {
            Set<ToolContract.ExecutorBinding> missingHandlers = EnumSet.copyOf(declaredBindings);
            missingHandlers.removeAll(availableBindings);
            Set<ToolContract.ExecutorBinding> orphanHandlers = EnumSet.copyOf(availableBindings);
            orphanHandlers.removeAll(declaredBindings);
            throw new IllegalStateException(
                "工具执行绑定不一致，缺少执行器=" + missingHandlers + "，孤立执行器=" + orphanHandlers
            );
        }
    }
    
    // ========== Validator Helper Methods ==========
    
    /**
     * 验证无参数工具
     */
    private Map<String, Object> validateNoParams(String argsJson) throws ToolContract.ValidationException {
        try {
            JsonNode args = objectMapper.readTree(argsJson);
            if (!args.isObject()) {
                throw new ToolContract.ValidationException("arguments", "参数必须是JSON对象");
            }
            if (args.size() > 0) {
                throw new ToolContract.ValidationException("arguments", "此工具不接受任何参数");
            }
            return new HashMap<>();
        } catch (ToolContract.ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolContract.ValidationException("arguments", "无效的JSON: " + e.getMessage());
        }
    }
    
    /**
     * 验证整数参数（带默认值和范围）
     */
    private Map<String, Object> validateIntegerParam(
            String argsJson, 
            String paramName, 
            int defaultValue, 
            int min, 
            int max) throws ToolContract.ValidationException {
        try {
            JsonNode args = objectMapper.readTree(argsJson);
            if (!args.isObject()) {
                throw new ToolContract.ValidationException("arguments", "参数必须是JSON对象");
            }
            
            // 保留具体字段路径，客户端才能定位并修正模型生成的错误参数。
            rejectUnknownFields(args, Set.of(paramName), "arguments");
            
            Map<String, Object> params = new HashMap<>();
            
            if (!args.has(paramName)) {
                params.put(paramName, defaultValue);
                return params;
            }
            
            JsonNode valueNode = args.get(paramName);
            if (!valueNode.isIntegralNumber()) {
                throw new ToolContract.ValidationException(paramName, "必须是整数");
            }
            
            int value = valueNode.intValue();
            if (value < min || value > max) {
                throw new ToolContract.ValidationException(
                    paramName, 
                    String.format("超出范围 [%d, %d]，实际值: %d", min, max, value)
                );
            }
            
            params.put(paramName, value);
            return params;
            
        } catch (ToolContract.ValidationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ToolContract.ValidationException("arguments", e.getMessage());
        } catch (Exception e) {
            throw new ToolContract.ValidationException("arguments", "无效的JSON: " + e.getMessage());
        }
    }
    
    /**
     * 验证可选日期参数
     */
    private Map<String, Object> validateOptionalDate(
            String argsJson,
            boolean allowFuture,
            int maxPastDays,
            int maxFutureDays) throws ToolContract.ValidationException {
        try {
            JsonNode args = objectMapper.readTree(argsJson);
            if (!args.isObject()) {
                throw new ToolContract.ValidationException("arguments", "参数必须是JSON对象");
            }
            
            // 保留具体字段路径，客户端才能定位并修正模型生成的错误参数。
            rejectUnknownFields(args, Set.of("date"), "arguments");
            
            Map<String, Object> params = new HashMap<>();
            
            String date;
            if (!args.has("date")) {
                date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            } else {
                JsonNode dateNode = args.get("date");
                if (!dateNode.isTextual() || dateNode.asText().isBlank()) {
                    throw new ToolContract.ValidationException("date", "必须是非空的yyyy-MM-dd字符串");
                }
                date = dateNode.asText();
            }
            
            // 验证日期格式和范围
            try {
                java.time.LocalDate requestDate = java.time.LocalDate.parse(date);
                java.time.LocalDate today = java.time.LocalDate.now();
                
                if (!allowFuture && requestDate.isAfter(today)) {
                    throw new ToolContract.ValidationException("date", "不能是未来日期: " + date);
                }
                
                if (allowFuture && requestDate.isAfter(today.plusDays(maxFutureDays))) {
                    throw new ToolContract.ValidationException("date", "超出未来日期范围: " + date);
                }
                
                if (requestDate.isBefore(today.minusDays(maxPastDays))) {
                    throw new ToolContract.ValidationException("date", "超出历史日期范围: " + date);
                }
            } catch (java.time.format.DateTimeParseException e) {
                throw new ToolContract.ValidationException("date", "格式错误，必须是yyyy-MM-dd: " + date);
            }
            
            params.put("date", date);
            return params;
            
        } catch (ToolContract.ValidationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ToolContract.ValidationException("arguments", e.getMessage());
        } catch (Exception e) {
            throw new ToolContract.ValidationException("arguments", "无效的JSON: " + e.getMessage());
        }
    }
    
    /**
     * 验证饮食摘要参数（date + rangeDays）
     */
    private Map<String, Object> validateDietSummary(String argsJson) throws ToolContract.ValidationException {
        try {
            JsonNode args = objectMapper.readTree(argsJson);
            if (!args.isObject()) {
                throw new ToolContract.ValidationException("arguments", "参数必须是JSON对象");
            }
            
            // 保留具体字段路径，客户端才能定位并修正模型生成的错误参数。
            rejectUnknownFields(args, Set.of("date", "rangeDays"), "arguments");
            
            Map<String, Object> params = new HashMap<>();
            
            // 验证date
            String date;
            if (!args.has("date")) {
                date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            } else {
                JsonNode dateNode = args.get("date");
                if (!dateNode.isTextual() || dateNode.asText().isBlank()) {
                    throw new ToolContract.ValidationException("date", "必须是非空的yyyy-MM-dd字符串");
                }
                date = dateNode.asText();
                
                // 验证日期格式
                try {
                    java.time.LocalDate requestDate = java.time.LocalDate.parse(date);
                    java.time.LocalDate today = java.time.LocalDate.now();
                    
                    if (requestDate.isAfter(today)) {
                        throw new ToolContract.ValidationException("date", "不能是未来日期: " + date);
                    }
                    
                    if (requestDate.isBefore(today.minusDays(365))) {
                        throw new ToolContract.ValidationException("date", "超出历史范围(365天): " + date);
                    }
                } catch (java.time.format.DateTimeParseException e) {
                    throw new ToolContract.ValidationException("date", "格式错误，必须是yyyy-MM-dd: " + date);
                }
            }
            params.put("date", date);
            
            // 验证rangeDays
            int rangeDays = 1; // 默认1天
            if (args.has("rangeDays")) {
                JsonNode rangeNode = args.get("rangeDays");
                if (!rangeNode.isIntegralNumber()) {
                    throw new ToolContract.ValidationException("rangeDays", "必须是整数");
                }
                rangeDays = rangeNode.intValue();
                if (rangeDays < 1 || rangeDays > 90) {
                    throw new ToolContract.ValidationException("rangeDays", "超出范围 [1, 90]，实际值: " + rangeDays);
                }
            }
            params.put("rangeDays", rangeDays);
            
            return params;
            
        } catch (ToolContract.ValidationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ToolContract.ValidationException("arguments", e.getMessage());
        } catch (Exception e) {
            throw new ToolContract.ValidationException("arguments", "无效的JSON: " + e.getMessage());
        }
    }
    
    /**
     * 验证训练计划草案（强约束）
     */
    private Map<String, Object> validateTrainingPlanDraft(String argsJson) throws ToolContract.ValidationException {
        try {
            JsonNode args = objectMapper.readTree(argsJson);
            requireObject(args, "arguments");
            rejectUnknownFields(args, Set.of(
                "title", "description", "goal", "experience", "targetMuscles",
                "trainingDays", "trainingDuration", "equipment", "days"
            ), "arguments");

            requireNonBlankText(args, "title", "title");
            requireNonBlankText(args, "goal", "goal");
            requireNonBlankText(args, "experience", "experience");
            if (args.has("description")) {
                requireText(args.get("description"), "description", false);
                if (args.get("description").asText().length() > 80) {
                    throw new ToolContract.ValidationException("description", "超过80字限制");
                }
            }
            for (String field : List.of("targetMuscles", "trainingDuration", "equipment")) {
                if (args.has(field)) {
                    requireText(args.get(field), field, false);
                }
            }

            JsonNode trainingDays = requireField(args, "trainingDays", "trainingDays");
            if (!trainingDays.isIntegralNumber() || !trainingDays.canConvertToInt()
                    || trainingDays.intValue() < 1 || trainingDays.intValue() > 7) {
                throw new ToolContract.ValidationException("trainingDays", "必须是 [1, 7] 范围内的整数");
            }

            JsonNode days = requireField(args, "days", "days");
            if (!days.isArray() || days.isEmpty() || days.size() != trainingDays.intValue()) {
                throw new ToolContract.ValidationException("days", "必须是与trainingDays数量一致的非空数组");
            }
            for (int i = 0; i < days.size(); i++) {
                String dayPath = "days[" + i + "]";
                JsonNode day = days.get(i);
                requireObject(day, dayPath);
                rejectUnknownFields(day, Set.of("name", "focus", "exercises"), dayPath);
                requireNonBlankText(day, "name", dayPath + ".name");
                requireNonBlankText(day, "focus", dayPath + ".focus");
                JsonNode exercises = requireField(day, "exercises", dayPath + ".exercises");
                if (!exercises.isArray() || exercises.isEmpty() || exercises.size() > 5) {
                    throw new ToolContract.ValidationException(dayPath + ".exercises", "必须是包含1至5个动作的数组");
                }
                for (int j = 0; j < exercises.size(); j++) {
                    String exercisePath = dayPath + ".exercises[" + j + "]";
                    JsonNode exercise = exercises.get(j);
                    requireObject(exercise, exercisePath);
                    rejectUnknownFields(exercise, Set.of("name", "sets", "reps", "restTime", "notes"), exercisePath);
                    requireNonBlankText(exercise, "name", exercisePath + ".name");
                    JsonNode sets = requireField(exercise, "sets", exercisePath + ".sets");
                    if (!sets.isIntegralNumber() || !sets.canConvertToInt() || sets.intValue() < 1 || sets.intValue() > 10) {
                        throw new ToolContract.ValidationException(exercisePath + ".sets", "必须是 [1, 10] 范围内的整数");
                    }
                    String reps = requireNonBlankText(exercise, "reps", exercisePath + ".reps");
                    if (!reps.matches("\\d+(?:-\\d+)?次")) {
                        throw new ToolContract.ValidationException(exercisePath + ".reps", "必须是带“次”单位的数字或数字范围");
                    }
                    String restTime = requireNonBlankText(exercise, "restTime", exercisePath + ".restTime");
                    if (!restTime.matches("\\d+(?:秒|分钟)")) {
                        throw new ToolContract.ValidationException(exercisePath + ".restTime", "必须是带“秒”或“分钟”单位的数字");
                    }
                    if (exercise.has("notes")) {
                        String notes = requireText(exercise.get("notes"), exercisePath + ".notes", false);
                        if (notes.length() > 15) {
                            throw new ToolContract.ValidationException(exercisePath + ".notes", "超过15字限制");
                        }
                    }
                }
            }
            return rawArguments(args);
        } catch (ToolContract.ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolContract.ValidationException("arguments", "无效的JSON或格式错误: " + e.getMessage());
        }
    }

    /**
     * 验证饮食记录草案
     */
    private Map<String, Object> validateDietRecordDraft(String argsJson) throws ToolContract.ValidationException {
        try {
            JsonNode args = objectMapper.readTree(argsJson);
            requireObject(args, "arguments");
            rejectUnknownFields(args, Set.of("date", "records"), "arguments");
            if (!args.has("date") || args.get("date").isNull() || args.get("date").asText().isBlank()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) args).put(
                    "date",
                    java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                );
            }
            String date = requireNonBlankText(args, "date", "date");
            try {
                java.time.LocalDate.parse(date);
            } catch (java.time.format.DateTimeParseException e) {
                throw new ToolContract.ValidationException("date", "格式错误，必须是yyyy-MM-dd: " + date);
            }

            JsonNode records = requireField(args, "records", "records");
            if (!records.isArray() || records.isEmpty()) {
                throw new ToolContract.ValidationException("records", "必须是至少包含一条记录的数组");
            }
            for (int i = 0; i < records.size(); i++) {
                String recordPath = "records[" + i + "]";
                JsonNode record = records.get(i);
                requireObject(record, recordPath);
                rejectUnknownFields(record, Set.of("meal_type", "food_name", "calories", "protein", "carbs", "fat", "amount", "is_estimated"), recordPath);
                String mealType = requireNonBlankText(record, "meal_type", recordPath + ".meal_type");
                if (!Set.of("早餐", "午餐", "晚餐", "加餐").contains(mealType)) {
                    throw new ToolContract.ValidationException(recordPath + ".meal_type", "必须是：早餐、午餐、晚餐、加餐之一");
                }
                requireNonBlankText(record, "food_name", recordPath + ".food_name");
                requireNonBlankText(record, "amount", recordPath + ".amount");
                JsonNode calories = requireField(record, "calories", recordPath + ".calories");
                if (!calories.isIntegralNumber() || !calories.canConvertToInt() || calories.intValue() < 0) {
                    throw new ToolContract.ValidationException(recordPath + ".calories", "必须是非负整数");
                }
                boolean estimated = record.has("is_estimated") && record.get("is_estimated").asBoolean(false);
                if (record.has("is_estimated") && !record.get("is_estimated").isBoolean()) {
                    throw new ToolContract.ValidationException(recordPath + ".is_estimated", "必须是布尔值");
                }
                if (estimated && calories.intValue() == 0) {
                    throw new ToolContract.ValidationException(recordPath + ".calories", "模型估算记录不能为0");
                }
                boolean hasPositiveMacro = false;
                for (String nutrient : List.of("protein", "carbs", "fat")) {
                    JsonNode value = requireField(record, nutrient, recordPath + "." + nutrient);
                    if (!value.isNumber() || !Double.isFinite(value.doubleValue()) || value.doubleValue() < 0) {
                        throw new ToolContract.ValidationException(recordPath + "." + nutrient, "必须是非负数值");
                    }
                    hasPositiveMacro |= value.doubleValue() > 0;
                }
                if (estimated && !hasPositiveMacro) {
                    throw new ToolContract.ValidationException(recordPath, "模型估算记录至少应包含一项大于0的营养素");
                }
            }
            return rawArguments(args);
        } catch (ToolContract.ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolContract.ValidationException("arguments", "无效的JSON或格式错误: " + e.getMessage());
        }
    }

    private Map<String, Object> rawArguments(JsonNode arguments) throws com.fasterxml.jackson.core.JsonProcessingException {
        return Map.of("_raw", objectMapper.writeValueAsString(arguments));
    }

    private JsonNode requireField(JsonNode object, String field, String path) throws ToolContract.ValidationException {
        if (!object.has(field) || object.get(field).isNull()) {
            throw new ToolContract.ValidationException(path, "缺少必填字段或字段为null");
        }
        return object.get(field);
    }

    private void requireObject(JsonNode node, String path) throws ToolContract.ValidationException {
        if (node == null || !node.isObject()) {
            throw new ToolContract.ValidationException(path, "必须是JSON对象");
        }
    }

    private String requireNonBlankText(JsonNode object, String field, String path) throws ToolContract.ValidationException {
        return requireText(requireField(object, field, path), path, true);
    }

    private String requireText(JsonNode node, String path, boolean nonBlank) throws ToolContract.ValidationException {
        if (!node.isTextual() || (nonBlank && node.asText().isBlank())) {
            throw new ToolContract.ValidationException(path, nonBlank ? "必须是非空字符串" : "必须是字符串");
        }
        return node.asText();
    }

    private void rejectUnknownFields(JsonNode object, Set<String> allowed, String path) throws ToolContract.ValidationException {
        Iterator<String> names = object.fieldNames();
        while (names.hasNext()) {
            String field = names.next();
            if (!allowed.contains(field)) {
                throw new ToolContract.ValidationException(path + "." + field, "不支持的参数");
            }
        }
    }
}
