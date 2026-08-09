package com.fitness.server.service;

import com.fitness.server.agent.LlmClient;
import com.fitness.server.dto.agent.MemoryResponse;
import com.fitness.server.dto.agent.UpsertMemoryRequest;
import com.fitness.server.entity.AgentMemory;
import com.fitness.server.entity.AgentTask;
import com.fitness.server.mapper.AgentMemoryMapper;
import com.fitness.server.mapper.AgentTaskMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AgentMemoryService {
    private static final int CONTEXT_MEMORY_LIMIT = 8;
    private static final int RECENT_TURNS_LIMIT = 2;
    private static final int RECENT_TURN_CONTENT_LIMIT = 500;
    private final AgentMemoryMapper memoryMapper;
    private final AgentTaskMapper taskMapper;

    public AgentMemoryService(AgentMemoryMapper memoryMapper, AgentTaskMapper taskMapper) {
        this.memoryMapper = memoryMapper;
        this.taskMapper = taskMapper;
    }

    public List<LlmClient.Message> buildMinimalContext(Long userId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return List.of();
        long now = System.currentTimeMillis();
        List<LlmClient.Message> context = new ArrayList<>();
        List<AgentMemory> memories = memoryMapper.getActiveForContext(userId, sessionId, now, CONTEXT_MEMORY_LIMIT);
        for (AgentMemory memory : memories) {
            context.add(new LlmClient.Message("system", "会话记忆[" + memory.getMemoryType() + "]：" + memory.getContent()));
        }
        List<AgentTask> recentTasks = taskMapper.getRecentTasksBySession(userId, sessionId, RECENT_TURNS_LIMIT);
        Collections.reverse(recentTasks);
        for (AgentTask task : recentTasks) {
            addRecentTurn(context, "user", task.getUserContent());
            addRecentTurn(context, "assistant", task.getAssistantContent());
        }
        return context;
    }

    public MemoryResponse create(Long userId, UpsertMemoryRequest request) {
        validateExpiration(request.getExpiresAt());
        long now = System.currentTimeMillis();
        AgentMemory memory = new AgentMemory();
        memory.setUserId(userId);
        memory.setSessionId(blankToNull(request.getSessionId()));
        memory.setMemoryType(request.getMemoryType());
        memory.setContent(request.getContent().trim());
        memory.setStatus("ACTIVE");
        memory.setExpiresAt(request.getExpiresAt());
        memory.setCreatedAt(now);
        memory.setUpdatedAt(now);
        memoryMapper.insert(memory);
        return toResponse(memory);
    }

    public MemoryResponse update(Long userId, Long memoryId, UpsertMemoryRequest request) {
        validateExpiration(request.getExpiresAt());
        int changed = memoryMapper.updateManagedMemory(memoryId, userId, request.getContent().trim(), request.getMemoryType(),
            request.getExpiresAt(), System.currentTimeMillis());
        if (changed == 0) throw new IllegalArgumentException("记忆不存在、已删除或无权访问");
        return toResponse(memoryMapper.getByIdAndUserId(memoryId, userId));
    }

    public void delete(Long userId, Long memoryId) {
        if (memoryMapper.softDelete(memoryId, userId, System.currentTimeMillis()) == 0) {
            throw new IllegalArgumentException("记忆不存在、已删除或无权访问");
        }
    }

    public List<MemoryResponse> list(Long userId, String sessionId) {
        return memoryMapper.listByUser(userId, blankToNull(sessionId), 100, 0).stream().map(this::toResponse).toList();
    }

    private void addRecentTurn(List<LlmClient.Message> context, String role, String content) {
        if (content == null || content.isBlank()) return;
        String clipped = content.length() <= RECENT_TURN_CONTENT_LIMIT ? content : content.substring(0, RECENT_TURN_CONTENT_LIMIT) + "…";
        context.add(new LlmClient.Message(role, clipped));
    }

    private void validateExpiration(Long expiresAt) {
        if (expiresAt != null && expiresAt <= System.currentTimeMillis()) throw new IllegalArgumentException("过期时间必须晚于当前时间");
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }

    private MemoryResponse toResponse(AgentMemory memory) {
        MemoryResponse response = new MemoryResponse();
        response.setId(memory.getId()); response.setSessionId(memory.getSessionId()); response.setMemoryType(memory.getMemoryType());
        response.setContent(memory.getContent()); response.setStatus(memory.getStatus()); response.setExpiresAt(memory.getExpiresAt());
        response.setCreatedAt(memory.getCreatedAt()); response.setUpdatedAt(memory.getUpdatedAt());
        return response;
    }
}
