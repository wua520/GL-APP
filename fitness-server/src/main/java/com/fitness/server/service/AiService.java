package com.fitness.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.server.dto.AiChatRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AiService {
    
    @Value("${deepseek.api-key}")
    private String apiKey;
    
    @Value("${deepseek.api-url}")
    private String apiUrl;
    
    @Value("${deepseek.model}")
    private String model;
    
    @Value("${deepseek.timeout}")
    private int timeout;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String SYSTEM_PROMPT = """
你是健录App的智能健身助手，专门为用户提供健身、训练、饮食和身体管理方面的建议。

你的职责：
1. 回答健身相关问题（动作要领、训练计划、肌肉锻炼等）
2. 提供饮食建议（热量摄入、蛋白质补充、营养搭配等）
3. 帮助用户分析训练数据和身体数据
4. 提供科学的健身知识

请用简洁、专业、友好的语气回答，避免过长的回复。
""";
    
    public String chat(List<AiChatRequest.Message> userMessages) throws Exception {
        // 构建完整的消息列表（包含系统提示）
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        
        for (AiChatRequest.Message msg : userMessages) {
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);
        
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
            throw new RuntimeException("DeepSeek API返回错误: " + responseCode);
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
        Map<String, Object> responseMap = objectMapper.readValue(response.toString(), Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            return (String) message.get("content");
        }
        
        throw new RuntimeException("无法解析AI响应");
    }
    
    public String chatStream(List<AiChatRequest.Message> userMessages) throws Exception {
        // 构建完整的消息列表
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        
        for (AiChatRequest.Message msg : userMessages) {
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }
        
        // 构建请求体（流式）
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);
        requestBody.put("stream", true);
        
        String jsonRequest = objectMapper.writeValueAsString(requestBody);
        
        // 发送HTTP请求
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setDoOutput(true);
        conn.setConnectTimeout(timeout * 1000);
        conn.setReadTimeout(timeout * 1000);
        
        // 发送请求
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // 读取流式响应
        StringBuilder fullResponse = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    
                    try {
                        Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            Map<String, Object> choice = choices.get(0);
                            Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                            if (delta != null && delta.containsKey("content")) {
                                String content = (String) delta.get("content");
                                fullResponse.append(content);
                            }
                        }
                    } catch (Exception e) {
                        // 忽略解析错误的chunk
                    }
                }
            }
        }
        
        return fullResponse.toString();
    }
}
