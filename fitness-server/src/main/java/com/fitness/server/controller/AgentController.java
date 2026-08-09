package com.fitness.server.controller;

import com.fitness.server.dto.ApiResponse;
import com.fitness.server.dto.agent.ConfirmActionRequest;
import com.fitness.server.dto.agent.CreateTaskRequest;
import com.fitness.server.dto.agent.TaskResponse;
import com.fitness.server.dto.agent.MemoryResponse;
import com.fitness.server.dto.agent.UpsertMemoryRequest;
import com.fitness.server.service.AgentMemoryService;
import com.fitness.server.service.AgentOrchestrator;
import com.fitness.server.service.AgentTaskEventService;
import com.fitness.server.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;

/**
 * Agent API 控制器
 * 负责接收客户端的Agent任务请求
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController extends BaseController {
    
    @Autowired
    private AgentOrchestrator agentOrchestrator;

    @Autowired
    private AgentMemoryService memoryService;

    @Autowired
    private AgentTaskEventService taskEventService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 创建Agent任务
     * 
     * @param token JWT Token（从Authorization header获取）
     * @param request 任务创建请求
     * @return 任务响应
     */
    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateTaskRequest request) {
        try {
            // 统一认证处理
            Long userId = extractUserId(token);
            
            // 调用Agent编排器执行任务
            TaskResponse response = agentOrchestrator.executeTask(userId, request);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            // 401/403 认证/授权错误
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getStatusCode().value(), e.getReason()));
        } catch (IllegalArgumentException e) {
            // 400 业务参数错误
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            // 500 服务器错误
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "任务执行失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/memories")
    public ResponseEntity<ApiResponse<java.util.List<MemoryResponse>>> listMemories(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String sessionId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(memoryService.list(extractUserId(token), sessionId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "查询记忆失败: " + e.getMessage()));
        }
    }

    @PostMapping("/memories")
    public ResponseEntity<ApiResponse<MemoryResponse>> createMemory(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpsertMemoryRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(memoryService.create(extractUserId(token), request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "创建记忆失败: " + e.getMessage()));
        }
    }

    @PutMapping("/memories/{memoryId}")
    public ResponseEntity<ApiResponse<MemoryResponse>> updateMemory(
            @RequestHeader("Authorization") String token,
            @PathVariable Long memoryId,
            @Valid @RequestBody UpsertMemoryRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(memoryService.update(extractUserId(token), memoryId, request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "更新记忆失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/memories/{memoryId}")
    public ResponseEntity<ApiResponse<String>> deleteMemory(
            @RequestHeader("Authorization") String token,
            @PathVariable Long memoryId) {
        try {
            memoryService.delete(extractUserId(token), memoryId);
            return ResponseEntity.ok(ApiResponse.success("记忆已删除"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "删除记忆失败: " + e.getMessage()));
        }
    }


    /**
     * 可选任务事件流。关闭时返回 404，客户端继续使用同步查询接口。
     */
    @GetMapping(value = "/tasks/{taskId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter taskEvents(
            @RequestHeader("Authorization") String token,
            @PathVariable Long taskId) {
        Long userId = extractUserId(token);
        if (!taskEventService.isEnabled()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "任务事件流未启用");
        }
        return taskEventService.subscribe(userId, taskId);
    }

    /**
     * 查询指定任务。
     *
     * @param token JWT Token
     * @param taskId 任务ID
     * @return 任务响应
     */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @RequestHeader("Authorization") String token,
            @PathVariable Long taskId) {
        try {
            // 统一认证处理
            Long userId = extractUserId(token);
            
            // 查询任务（安全检查：确保任务属于该用户）
            TaskResponse response = agentOrchestrator.getTask(userId, taskId);
            
            if (response == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "任务不存在或无权访问"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getStatusCode().value(), e.getReason()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "查询失败: " + e.getMessage()));
        }
    }
    
    /**
     * 确认操作（执行写入）
     * 
     * @param token JWT Token
     * @param taskId 任务ID
     * @param request 确认请求
     * @return 执行结果
     */
    @PostMapping("/tasks/{taskId}/confirm")
    public ResponseEntity<ApiResponse<TaskResponse>> confirmAction(
            @RequestHeader("Authorization") String token,
            @PathVariable Long taskId,
            @Valid @RequestBody ConfirmActionRequest request) {
        try {
            // 统一认证处理
            Long userId = extractUserId(token);
            
            // 执行确认操作
            TaskResponse response = agentOrchestrator.confirmAction(userId, taskId, request.getActionId());
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (UnsupportedOperationException e) {
            // 功能未启用
            return ResponseEntity.status(501)
                    .body(ApiResponse.error(501, e.getMessage()));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getStatusCode().value(), e.getReason()));
        } catch (IllegalStateException e) {
            // 业务状态异常（任务已取消等）
            return ResponseEntity.status(409)  // 409 Conflict
                    .body(ApiResponse.error(409, e.getMessage()));
        } catch (IllegalArgumentException e) {
            // 业务异常（草案过期、已执行等）
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "确认失败: " + e.getMessage()));
        }
    }
    
    /**
     * 取消操作
     * 
     * @param token JWT Token
     * @param taskId 任务ID
     * @return 取消结果
     */
    @PostMapping("/tasks/{taskId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelTask(
            @RequestHeader("Authorization") String token,
            @PathVariable Long taskId) {
        try {
            // 统一认证处理
            Long userId = extractUserId(token);
            
            // 取消任务
            agentOrchestrator.cancelTask(userId, taskId);
            
            return ResponseEntity.ok(ApiResponse.success("任务已取消"));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getStatusCode().value(), e.getReason()));
        } catch (IllegalStateException e) {
            // 业务状态异常（任务已完成、不允许取消等）
            return ResponseEntity.status(409)  // 409 Conflict
                    .body(ApiResponse.error(409, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "取消失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/tasks/{taskId}/actions/{actionId}/cancel")
    public ResponseEntity<ApiResponse<TaskResponse>> cancelAction(
            @RequestHeader("Authorization") String token,
            @PathVariable Long taskId,
            @PathVariable Long actionId) {
        try {
            Long userId = extractUserId(token);
            return ResponseEntity.ok(ApiResponse.success(agentOrchestrator.cancelAction(userId, taskId, actionId)));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getStatusCode().value(), e.getReason()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(409, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "取消失败: " + e.getMessage()));
        }
    }

    /**
     * 更新操作草案（编辑功能）
     * 
     * @param token JWT Token
     * @param taskId 任务ID
     * @param actionId 操作ID
     * @param request 更新请求（包含新的 payloadJson）
     * @return 更新后的任务响应
     */
    @PutMapping("/tasks/{taskId}/actions/{actionId}/payload")
    public ResponseEntity<ApiResponse<TaskResponse>> updateActionPayload(
            @RequestHeader("Authorization") String token,
            @PathVariable Long taskId,
            @PathVariable Long actionId,
            @Valid @RequestBody com.fitness.server.dto.agent.UpdatePayloadRequest request) {
        try {
            // 统一认证处理
            Long userId = extractUserId(token);
            
            // 更新草案
            TaskResponse response = agentOrchestrator.updateActionPayload(
                userId, 
                taskId, 
                actionId, 
                request.getPayloadJson()
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getStatusCode().value(), e.getReason()));
        } catch (IllegalStateException e) {
            // 业务状态异常（只有 WAITING_CONFIRMATION 可编辑）
            return ResponseEntity.status(409)  // 409 Conflict
                    .body(ApiResponse.error(409, e.getMessage()));
        } catch (IllegalArgumentException e) {
            // payload 格式错误
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 完成本地写入（两阶段提交第二阶段）
     * 
     * @param token JWT Token
     * @param taskId 任务ID
     * @param actionId 操作ID
     * @param request 完成请求（包含本地引用ID）
     * @return 任务响应
     */
    @PostMapping("/tasks/{taskId}/actions/{actionId}/complete")
    public ResponseEntity<ApiResponse<TaskResponse>> completeLocalWrite(
            @RequestHeader("Authorization") String token,
            @PathVariable Long taskId,
            @PathVariable Long actionId,
            @Valid @RequestBody com.fitness.server.dto.agent.CompleteLocalWriteRequest request) {
        try {
            // 统一认证处理
            Long userId = extractUserId(token);
            
            if (request.getActionId() != null && !actionId.equals(request.getActionId())) {
                throw new IllegalArgumentException("请求体中的操作ID与请求路径不一致");
            }
            if (request.getLocalReference() == null || request.getLocalReference().isBlank()) {
                throw new IllegalArgumentException("本地引用不能为空");
            }

            // 完成本地写入
            TaskResponse response = agentOrchestrator.completeLocalWrite(
                userId, 
                taskId, 
                actionId, 
                request.getLocalReference()
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getStatusCode().value(), e.getReason()));
        } catch (IllegalStateException e) {
            // 业务状态异常
            return ResponseEntity.status(409)  // 409 Conflict
                    .body(ApiResponse.error(409, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "完成失败: " + e.getMessage()));
        }
    }
    
    /**
     * 查询用户所有待完成本地写入的操作（用于恢复）
     * 
     * @param token JWT Token
     * @return 待完成操作列表
     */
    @GetMapping("/pending-local-writes")
    public ResponseEntity<ApiResponse<java.util.List<com.fitness.server.dto.agent.PendingLocalWriteResponse>>> getPendingLocalWrites(
            @RequestHeader("Authorization") String token) {
        try {
            // 统一认证处理
            Long userId = extractUserId(token);
            
            // 查询待完成操作
            java.util.List<com.fitness.server.dto.agent.PendingLocalWriteResponse> pendingWrites = 
                agentOrchestrator.getPendingLocalWrites(userId);
            
            return ResponseEntity.ok(ApiResponse.success(pendingWrites));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getStatusCode().value(), e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "查询失败: " + e.getMessage()));
        }
    }
}
