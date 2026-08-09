package com.fitness.server.controller;

import com.fitness.server.dto.AiChatRequest;
import com.fitness.server.dto.AiChatResponse;
import com.fitness.server.dto.ApiResponse;
import com.fitness.server.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    
    @Autowired
    private AiService aiService;
    
    /**
     * AI聊天接口（普通模式）
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @RequestHeader("Authorization") String token,
            @RequestBody AiChatRequest request) {
        try {
            String response = aiService.chat(request.getMessages());
            AiChatResponse chatResponse = new AiChatResponse(response);
            return ResponseEntity.ok(ApiResponse.success(chatResponse));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "AI服务异常: " + e.getMessage()));
        }
    }
    
    /**
     * AI聊天接口（流式模式）
     */
    @PostMapping("/chat/stream")
    public ResponseEntity<ApiResponse<AiChatResponse>> chatStream(
            @RequestHeader("Authorization") String token,
            @RequestBody AiChatRequest request) {
        try {
            String response = aiService.chatStream(request.getMessages());
            AiChatResponse chatResponse = new AiChatResponse(response);
            return ResponseEntity.ok(ApiResponse.success(chatResponse));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "AI服务异常: " + e.getMessage()));
        }
    }
}
