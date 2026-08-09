package com.fitness.server.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * LLM 客户端
 * 负责与DeepSeek模型通信，支持Function Calling
 * 
 * 职责：
 * 1. 发送消息到DeepSeek
 * 2. 处理工具调用响应
 * 3. 管理对话上下文
 */
@Component
public class LlmClient {
    
    @Value("${deepseek.api-key}")
    private String apiKey;
    
    @Value("${deepseek.api-url}")
    private String apiUrl;
    
    @Value("${deepseek.model}")
    private String model;
    
    @Value("${deepseek.timeout}")
    private int timeout;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Agent系统提示词
     */
    private static final String SYSTEM_PROMPT = """
你是健录App的智能健身Agent。

你的能力：
- 查询用户的训练、饮食、身体数据（通过工具）
- 分析用户的健身进展和恢复状态
- 生成训练计划草案（需用户确认）
- 生成饮食记录草案（需用户确认）

你的限制：
- 不能直接修改用户数据，所有写入需生成草案等待确认
- 不能访问其他用户的数据
- 数据不足时必须询问用户

工作流程：
1. 理解用户意图
2. 判断需要哪些数据
3. 调用工具获取数据（按需获取，不要一次性获取所有数据）
4. 基于数据给出建议
5. 如果需要创建训练计划，调用create_training_plan_draft工具生成草案

创建训练计划的流程：
1. 先了解用户的目标、经验水平、可训练天数、可用器械和身体限制
2. 根据用户情况设计合理的训练计划
3. **向用户展示简化概要**（可以用Markdown表格显示主要动作）
4. **同时调用create_training_plan_draft工具**，传入完整的JSON结构（包含每个动作的sets、reps、restTime、notes）
5. 工具返回成功后，告诉用户"计划已生成，等待确认后即可使用"

**关键**：展示和工具调用是分开的
- 给用户看：简化表格（方便阅读）
- 调用工具：完整JSON（包含所有细节）

**正确示例**：
```
用户消息："帮我制定增肌计划，中级，4天"

AI回复给用户（Markdown表格）：
好的！我为你设计了4天分化增肌计划：

| 天数 | 重点 | 主要动作 |
|------|------|----------|
| 第1天 | 胸+三头 | 杠铃卧推、上斜卧推、双杠臂屈伸 |
| 第2天 | 背+二头 | 杠铃划船、引体向上、杠铃弯举 |

计划已生成，请确认后即可使用 💪

同时调用工具（完整JSON）：
{
  "title": "4天分化增肌计划",
  "goal": "增肌",
  "trainingDays": 4,
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
          "notes": "控制节奏"
        },
        {
          "name": "上斜卧推",
          "sets": 3,
          "reps": "10-12次",
          "restTime": "60秒",
          "notes": "上胸刺激"
        }
      ]
    }
  ]
}
```

生成训练计划草案的约束规则：
- 每天最多5个主要动作（核心复合动作优先）
- 每个动作的notes不超过15字（简明扼要）
- description总字数不超过80字
- 避免重复描述相同概念
- reps必须带"次"单位（如"8-12次"）
- restTime必须带时间单位（如"90秒"、"2分钟"）
- **工具调用的JSON必须包含完整的exercises数组，每个exercise必须有name、sets、reps、restTime、notes五个字段**

医疗安全边界（严格遵守）：
- **不做医疗诊断**：不能诊断疾病、病因或病情
- **疼痛/不适**：遇到疼痛、头晕、胸痛、呼吸困难、关节肿胀等身体不适，必须建议就医
- **疾病相关**：遇到高血压、糖尿病、心脏病、关节炎等疾病咨询，说明需要医生指导，不能给出训练或饮食方案
- **药物相关**：不能推荐或评论任何药物（处方药、非处方药），涉及药物的问题必须建议咨询医生或药师
- **补剂安全**：
  - 可以讨论常见蛋白粉、维生素等基础补剂的一般用途
  - 不能推荐促睾、激素类、未经验证的减肥药等危险补剂
  - 涉及特殊补剂或大剂量使用，建议咨询营养师或医生
  - 明确说明"补剂不能替代均衡饮食"
- **伤病训练**：有伤病史的用户询问能否训练，必须建议先咨询医生或康复师

数据边界：
- 不编造数据
- 明确区分：事实（来自工具）、推断（基于数据的分析）、建议（个性化建议）、待确认操作（draft）
- 只有调用draft工具并收到成功响应，才能说"已生成草案"或"等待确认"
- 不能说"已创建"或"已保存"，只有用户确认后系统才会真正保存

回复风格：
- 简洁专业，避免冗长
- 基于真实数据，不要泛泛而谈
- 具体可行（例如"增加10kg重量"而不是"多练"）
""";
    
    /**
     * 发送聊天请求（支持Function Calling）
     * 
     * @param messages 消息列表
     * @param tools 可用工具列表（可选）
     * @return LLM响应
     */
    public LlmResponse chat(List<Message> messages, List<Tool> tools) throws Exception {
        return chat(messages, tools, null);
    }

    /**
     * 可选强制工具调用。记录类写入不能接受模型只返回自然语言。
     */
    public LlmResponse chat(List<Message> messages, List<Tool> tools, String forcedToolName) throws Exception {
        // 构建完整的消息列表（包含系统提示）
        List<Map<String, Object>> fullMessages = new ArrayList<>();
        fullMessages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        
        for (Message msg : messages) {
            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("role", msg.getRole());
            
            // content可能为null（当assistant只有tool_calls时）
            if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                msgMap.put("content", msg.getContent());
            }
            
            // 如果是assistant的tool_calls
            if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                List<Map<String, Object>> toolCallsList = new ArrayList<>();
                for (ToolCall tc : msg.getToolCalls()) {
                    Map<String, Object> tcMap = new HashMap<>();
                    tcMap.put("id", tc.getId());
                    tcMap.put("type", "function");
                    Map<String, Object> function = new HashMap<>();
                    function.put("name", tc.getName());
                    function.put("arguments", tc.getArguments());
                    tcMap.put("function", function);
                    toolCallsList.add(tcMap);
                }
                msgMap.put("tool_calls", toolCallsList);
            }
            
            // 如果是工具调用结果
            if (msg.getToolCallId() != null) {
                msgMap.put("tool_call_id", msg.getToolCallId());
            }
            
            fullMessages.add(msgMap);
        }
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", fullMessages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 16384);  // 设置为16K，留足余量
        
        // 如果有工具，添加到请求中
        if (tools != null && !tools.isEmpty()) {
            List<Map<String, Object>> toolDefs = new ArrayList<>();
            for (Tool tool : tools) {
                toolDefs.add(tool.toMap());
            }
            requestBody.put("tools", toolDefs);
            if (forcedToolName != null && !forcedToolName.isBlank()) {
                requestBody.put("tool_choice", Map.of(
                    "type", "function",
                    "function", Map.of("name", forcedToolName)
                ));
            } else {
                requestBody.put("tool_choice", "auto"); // 让模型自动决定是否调用工具
            }
        }
        
        String jsonRequest = objectMapper.writeValueAsString(requestBody);
        
        // 发送HTTP请求
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(timeout * 1000);
        conn.setReadTimeout(timeout * 1000);
        
        // 发送请求
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // 读取响应
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            // 读取错误信息
            String errorMessage = "";
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder errorBuilder = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    errorBuilder.append(line);
                }
                errorMessage = errorBuilder.toString();
            } catch (Exception e) {
                errorMessage = "无法读取错误详情";
            }
            throw new RuntimeException("DeepSeek API返回错误 " + responseCode + ": " + errorMessage);
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }
        
        // 解析响应
        return parseResponse(response.toString());
    }
    
    /**
     * 解析LLM响应
     */
    private LlmResponse parseResponse(String jsonResponse) throws Exception {
        Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        
        // 防御性检查：choices为空数组
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("LLM响应缺少choices字段或为空数组");
        }
        
        Map<String, Object> choice = choices.get(0);
        if (choice == null) {
            throw new RuntimeException("LLM响应的choice为null");
        }
        
        Map<String, Object> message = (Map<String, Object>) choice.get("message");
        if (message == null) {
            throw new RuntimeException("LLM响应的message为null");
        }
        
        LlmResponse llmResponse = new LlmResponse();
        
        // 检查是否有文本内容
        String content = (String) message.get("content");
        if (content != null) {
            llmResponse.setContent(content);
        }
        llmResponse.setFinishReason((String) choice.get("finish_reason"));
        
        // 检查是否有工具调用
        List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
        if (toolCalls != null && !toolCalls.isEmpty()) {
            List<ToolCall> parsedToolCalls = new ArrayList<>();
            Set<String> toolCallIds = new HashSet<>();
            for (Map<String, Object> toolCall : toolCalls) {
                String id = (String) toolCall.get("id");
                String type = (String) toolCall.get("type");
                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                
                if (!"function".equals(type) || function == null || id == null || id.isBlank()) {
                    throw new RuntimeException("LLM返回了无效的工具调用");
                }
                if (!toolCallIds.add(id)) {
                    throw new RuntimeException("LLM返回了重复的工具调用ID: " + id);
                }
                String name = (String) function.get("name");
                String arguments = (String) function.get("arguments");
                if (name == null || name.isBlank() || arguments == null || !isJsonObject(arguments)) {
                    throw new RuntimeException("LLM工具调用缺少名称或合法JSON对象参数");
                }
                ToolCall tc = new ToolCall();
                tc.setId(id);
                tc.setName(name);
                tc.setArguments(arguments);
                parsedToolCalls.add(tc);
            }
            llmResponse.setToolCalls(parsedToolCalls);
        }
        
        return llmResponse;
    }

    private boolean isJsonObject(String json) {
        try {
            return objectMapper.readTree(json).isObject();
        } catch (Exception ignored) {
            return false;
        }
    }
    
    /**
     * LLM消息
     */
    public static class Message {
        private String role; // user, assistant, system, tool
        private String content;
        private String toolCallId; // 如果是工具返回结果
        private List<ToolCall> toolCalls; // 如果是assistant调用工具
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public Message(String role, String content, String toolCallId) {
            this.role = role;
            this.content = content;
            this.toolCallId = toolCallId;
        }
        
        public Message(String role, String content, List<ToolCall> toolCalls) {
            this.role = role;
            this.content = content;
            this.toolCalls = toolCalls;
        }
        
        // Getters and Setters
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getToolCallId() { return toolCallId; }
        public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }
        public List<ToolCall> getToolCalls() { return toolCalls; }
        public void setToolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; }
    }
    
    /**
     * LLM响应
     */
    public static class LlmResponse {
        private String content;
        private String finishReason;
        private List<ToolCall> toolCalls;
        
        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
        
        // Getters and Setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
        public List<ToolCall> getToolCalls() { return toolCalls; }
        public void setToolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; }
    }
    
    /**
     * 工具调用
     */
    public static class ToolCall {
        private String id;
        private String name;
        private String arguments; // JSON格式的参数
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getArguments() { return arguments; }
        public void setArguments(String arguments) { this.arguments = arguments; }
    }
    
    /**
     * 工具定义
     */
    public static class Tool {
        private String name;
        private String description;
        private Map<String, Object> parameters;
        private String category; // READ | DRAFT | WRITE
        
        public Tool(String name, String description, Map<String, Object> parameters) {
            this(name, description, parameters, "READ"); // 默认为READ
        }
        
        public Tool(String name, String description, Map<String, Object> parameters, String category) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
            this.category = category;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> toolMap = new HashMap<>();
            toolMap.put("type", "function");
            
            Map<String, Object> function = new HashMap<>();
            function.put("name", name);
            function.put("description", description);
            function.put("parameters", parameters);
            
            toolMap.put("function", function);
            return toolMap;
        }
        
        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Map<String, Object> getParameters() { return parameters; }
        public String getCategory() { return category; }
    }
}
