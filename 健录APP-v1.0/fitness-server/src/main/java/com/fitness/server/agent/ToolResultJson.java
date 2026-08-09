package com.fitness.server.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LLM 工具循环的统一错误结果编码。
 */
public final class ToolResultJson {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ToolResultJson() {
    }

    public static String error(String code, String message, boolean retryable) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", code);
        payload.put("message", message == null ? "未提供错误详情" : message);
        payload.put("retryable", retryable);
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception ignored) {
            return "{\"error\":\"serialization_failure\",\"message\":\"工具结果序列化失败\",\"retryable\":false}";
        }
    }
}
