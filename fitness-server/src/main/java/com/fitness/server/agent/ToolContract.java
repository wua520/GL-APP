package com.fitness.server.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/**
 * 工具契约 - 唯一事实源
 * 
 * 所有工具的定义、Schema、校验规则、执行绑定都从这里获取
 * 禁止在其他地方重复定义工具Schema
 * 
 * Phase G: Tool Contract Unification
 */
public class ToolContract {
    
    private final String name;
    private final String description;
    private final ToolCategory category;
    private final Map<String, Object> inputSchema;
    private final ToolValidator validator;
    private final String domainOwner; // "training", "nutrition", "progress"
    private final ExecutorBinding executorBinding;
    private final boolean requiresConfirmation;

    /**
     * 执行语义的受限标识。工具名可面向模型演进，执行目标必须显式且可校验。
     */
    public enum ExecutorBinding {
        TRAINING_SUMMARY,
        RECENT_WORKOUTS,
        ACTIVE_TRAINING_PLAN,
        TRAINING_SCHEDULE,
        TRAINING_PROGRESS,
        RECOVERY_STATUS,
        CREATE_TRAINING_PLAN_DRAFT,
        TODAY_DIET_SUMMARY,
        DIET_SUMMARY,
        CREATE_DIET_RECORD_DRAFT,
        USER_FITNESS_PROFILE,
        DAILY_NUTRITION_PROGRESS,
        BODY_TREND
    }
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public enum ToolCategory {
        READ,      // 查询工具
        DRAFT,     // 草案工具（需要用户确认）
        WRITE      // 直接写入（暂不使用）
    }
    
    private ToolContract(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.category = builder.category;
        this.inputSchema = strictSchema(builder.inputSchema);
        this.validator = builder.validator;
        this.domainOwner = builder.domainOwner;
        this.executorBinding = builder.executorBinding;
        this.requiresConfirmation = builder.category == ToolCategory.DRAFT;
    }

    private static Map<String, Object> strictSchema(Map<String, Object> schema) {
        return Collections.unmodifiableMap(copySchemaObject(schema));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copySchemaObject(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), copySchemaValue(entry.getValue()));
        }
        if ("object".equals(copy.get("type"))) {
            copy.put("additionalProperties", false);
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static Object copySchemaValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return Collections.unmodifiableMap(copySchemaObject((Map<String, Object>) map));
        }
        if (value instanceof List<?> list) {
            return List.copyOf(list.stream().map(ToolContract::copySchemaValue).toList());
        }
        return value;
    }
    
    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ToolCategory getCategory() { return category; }
    public Map<String, Object> getInputSchema() { return inputSchema; }
    public ToolValidator getValidator() { return validator; }
    public String getDomainOwner() { return domainOwner; }
    public ExecutorBinding getExecutorBinding() { return executorBinding; }
    public boolean requiresConfirmation() { return requiresConfirmation; }
    
    /**
     * 转换为LLM可见的Tool对象
     */
    public com.fitness.server.agent.LlmClient.Tool toLlmTool() {
        return new com.fitness.server.agent.LlmClient.Tool(
            name,
            description,
            inputSchema,
            category.name()
        );
    }
    
    /**
     * 验证参数
     */
    public Map<String, Object> validateAndNormalize(String argumentsJson) throws ValidationException {
        return validator.validate(argumentsJson);
    }
    
    /**
     * Builder
     */
    public static class Builder {
        private String name;
        private String description;
        private ToolCategory category;
        private Map<String, Object> inputSchema;
        private ToolValidator validator;
        private String domainOwner;
        private ExecutorBinding executorBinding;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder category(ToolCategory category) {
            this.category = category;
            return this;
        }
        
        public Builder inputSchema(Map<String, Object> schema) {
            this.inputSchema = schema;
            return this;
        }
        
        public Builder validator(ToolValidator validator) {
            this.validator = validator;
            return this;
        }
        
        public Builder domainOwner(String domain) {
            this.domainOwner = domain;
            return this;
        }

        public Builder executorBinding(ExecutorBinding binding) {
            this.executorBinding = binding;
            return this;
        }
        
        public ToolContract build() {
            Objects.requireNonNull(name, "Tool name is required");
            Objects.requireNonNull(description, "Tool description is required");
            Objects.requireNonNull(category, "Tool category is required");
            Objects.requireNonNull(inputSchema, "Tool schema is required");
            Objects.requireNonNull(validator, "Tool validator is required");
            Objects.requireNonNull(domainOwner, "Tool domain owner is required");
            Objects.requireNonNull(executorBinding, "Tool executor binding is required");
            return new ToolContract(this);
        }
    }
    
    /**
     * 工具验证器接口
     */
    @FunctionalInterface
    public interface ToolValidator {
        /**
         * 验证并规范化参数
         * 
         * @param argumentsJson JSON字符串参数
         * @return 规范化后的参数Map
         * @throws ValidationException 验证失败
         */
        Map<String, Object> validate(String argumentsJson) throws ValidationException;
    }
    
    /**
     * 验证异常
     */
    public static class ValidationException extends Exception {
        private final String field;
        private final String reason;
        
        public ValidationException(String field, String reason) {
            super(String.format("Validation failed for field '%s': %s", field, reason));
            this.field = field;
            this.reason = reason;
        }
        
        public ValidationException(String message) {
            super(message);
            this.field = null;
            this.reason = message;
        }
        
        public String getField() { return field; }
        public String getReason() { return reason; }
        
        /**
         * 转换为JSON错误结果
         */
        public String toJsonError() {
            try {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "参数验证失败");
                error.put("field", field != null ? field : "unknown");
                error.put("reason", reason);
                error.put("retryable", true);
                return objectMapper.writeValueAsString(error);
            } catch (Exception e) {
                return "{\"error\":\"参数验证失败\",\"retryable\":true}";
            }
        }
    }
}
