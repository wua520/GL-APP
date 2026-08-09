package com.fitness.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.server.agent.AgentAuditService;
import com.fitness.server.agent.AgentToolExecutorV2;
import com.fitness.server.agent.AuthorizedToolResolver;
import com.fitness.server.agent.ToolContract;
import com.fitness.server.agent.ToolContractRegistry;
import com.fitness.server.agent.ToolResultJson;
import com.fitness.server.agent.LlmClient;
import com.fitness.server.dto.agent.CreateTaskRequest;
import com.fitness.server.dto.agent.TaskResponse;
import com.fitness.server.entity.AgentAction;
import com.fitness.server.entity.AgentAuditLog;
import com.fitness.server.entity.AgentTask;
import com.fitness.server.entity.TrainingPlan;
import com.fitness.server.mapper.AgentActionMapper;
import com.fitness.server.mapper.AgentAuditLogMapper;
import com.fitness.server.mapper.AgentTaskMapper;
import com.fitness.server.mapper.DietRecordMapper;
import com.fitness.server.mapper.TrainingPlanMapper;
import com.fitness.server.knowledge.KnowledgeAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Agent 编排器
 * 负责驱动Agent任务的生命周期
 * 
 * 职责：
 * 1. 创建任务并记录状态
 * 2. 调用LlmClient与模型交互
 * 3. 调用AgentToolExecutor执行工具
 * 4. 生成待确认草案
 * 5. 执行确认后的写入操作
 * 6. 记录审计日志
 */
@Service
public class AgentOrchestrator {
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentOrchestrator.class);
    
    @Autowired
    private AgentTaskMapper taskMapper;
    
    @Autowired
    private AgentActionMapper actionMapper;
    
    @Autowired
    private AgentAuditLogMapper auditLogMapper;
    
    @Autowired
    private LlmClient llmClient;
    
    @Autowired
    private AgentToolExecutorV2 toolExecutor;
    
    @Autowired
    private TrainingPlanMapper trainingPlanMapper;
    
    @Autowired
    private ToolContractRegistry toolContractRegistry;

    @Autowired
    private AuthorizedToolResolver authorizedToolResolver;
    
    @Autowired
    private AgentAuditService auditService;
    
    @Autowired
    private DietRecordMapper dietRecordMapper;
    
    @Autowired
    private com.fitness.server.agent.SupervisorService supervisorService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private KnowledgeAgent knowledgeAgent;

    @Autowired
    private AgentMemoryService memoryService;
    
    private static final int MAX_TOOL_ITERATIONS = 5; // 最大工具调用轮数
    private static final boolean ENABLE_SUPERVISOR = true;
    private static final ObjectMapper LOCAL_REFERENCE_MAPPER = new ObjectMapper();
    
    /**
     * F1-B 仅接管明确的健康领域请求；其余内容继续由稳定的通用链路处理。
     */
    private boolean shouldUseHealthSupervisor(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        String message = userMessage.toLowerCase();
        return containsTrainingDomain(message) || containsNutritionDomain(message) ||
            containsProgressDomain(message);
    }

    /**
     * 执行Agent任务
     * 
     * @param userId 用户ID（从JWT获取）
     * @param request 任务请求
     * @return 任务响应
     */
    public TaskResponse executeTask(Long userId, CreateTaskRequest request) {
        // F1-B 只接管已经识别为训练、饮食或进度的请求；其他消息保留既有通用链路。
        if (ENABLE_SUPERVISOR && shouldUseHealthSupervisor(request.getContent())) {
            try {
                com.fitness.server.agent.SupervisorService.SupervisorResult supervisorResult = 
                    supervisorService.planAndExecute(request.getContent(), userId, request.getSessionId());
                
                // 如果Supervisor已处理（BLOCKED或SUCCESS），返回Supervisor结果
                if ("BLOCKED".equals(supervisorResult.getStatus())) {
                    // 安全阻断：创建任务记录但不执行
                    AgentTask blockedTask = new AgentTask();
                    blockedTask.setUserId(userId);
                    blockedTask.setSessionId(request.getSessionId());
                    blockedTask.setUserContent(request.getContent());
                    blockedTask.setStatus("BLOCKED");
                    blockedTask.setCreatedAt(System.currentTimeMillis());
                    blockedTask.setAssistantContent(supervisorResult.getFinalMessage());
                    blockedTask.setCompletedAt(System.currentTimeMillis());
                    taskMapper.insert(blockedTask);
                    
                    return buildTaskResponse(blockedTask.getId(), userId);
                }
                
                // Supervisor 已完成编排；PARTIAL 结果中已生成的草案也必须保留，
                // 不能因无关分析不完整而丢弃用户可确认的待写入操作。
                if ("SUCCESS".equals(supervisorResult.getStatus()) ||
                    "PARTIAL".equals(supervisorResult.getStatus())) {
                    // Supervisor已完整处理（训练领域），转换为TaskResponse
                    return convertSupervisorResultToTaskResponse(supervisorResult, userId, request);
                }
                
                // Supervisor 是 F1-A 唯一入口。未知路由、失败和降级只能产生
                // 只读终态，绝不能回退到旧流程重新开放草案工具。
                return convertSupervisorResultToTaskResponse(supervisorResult, userId, request);
                
            } catch (Exception e) {
                logger.error("Supervisor unavailable; returning safe read-only failure", e);
                com.fitness.server.agent.SupervisorService.SupervisorResult safeFailure =
                    new com.fitness.server.agent.SupervisorService.SupervisorResult();
                safeFailure.setStatus("FAILED");
                safeFailure.setFinalMessage("训练助手暂时不可用，未创建任何待确认训练计划，请稍后重试。");
                return convertSupervisorResultToTaskResponse(safeFailure, userId, request);
            }
        }
        
        // 1. 创建任务记录
        AgentTask task = new AgentTask();
        task.setUserId(userId);
        task.setSessionId(request.getSessionId());
        task.setUserContent(request.getContent());
        task.setStatus("RECEIVED");
        task.setCreatedAt(System.currentTimeMillis());
        
        taskMapper.insert(task);
        
        try {
            // 2. 更新状态为 ANALYZING
            taskMapper.updateStatus(task.getId(), "ANALYZING", null);
            auditService.logAnalyzing(task.getId());
            
            // 3. 仅组装受限的结构化记忆和近期片段，避免全量会话历史无限进入模型上下文。
            List<LlmClient.Message> messages = new ArrayList<>(
                memoryService.buildMinimalContext(userId, request.getSessionId())
            );
            
            // 添加当前用户消息
            messages.add(new LlmClient.Message("user", request.getContent()));
            
            // 4. 修复：根据任务类型筛选可用工具
            List<LlmClient.Tool> allowedTools = determineAllowedTools(request.getContent());
            
            // 创建工具名称集合用于执行时验证（修复漏洞1）
            Set<String> allowedToolNames = allowedTools.stream()
                .map(LlmClient.Tool::getName)
                .collect(java.util.stream.Collectors.toSet());
            
            // 5. 多轮对话循环（支持工具调用）
            int iteration = 0;
            String finalResponse = null;
            boolean hasCalledTools = false;
            
            while (iteration < MAX_TOOL_ITERATIONS) {
                iteration++;
                
                // 调用LLM
                LlmClient.LlmResponse llmResponse = llmClient.chat(messages, allowedTools);
                
                // 如果有工具调用
                if (llmResponse.hasToolCalls()) {
                    if (!"tool_calls".equals(llmResponse.getFinishReason())) {
                        throw new IllegalStateException("LLM工具调用缺少tool_calls终止原因");
                    }
                    // 首次工具调用，更新状态
                    if (!hasCalledTools) {
                        taskMapper.updateStatus(task.getId(), "CALLING_TOOLS", null);
                        hasCalledTools = true;
                    }
                    
                    // 先添加assistant的响应（包含tool_calls）
                    messages.add(new LlmClient.Message("assistant", llmResponse.getContent(), llmResponse.getToolCalls()));
                    
                    List<DraftPayload> successfulDrafts = new ArrayList<>();
                    for (LlmClient.ToolCall toolCall : llmResponse.getToolCalls()) {
                        // 1. 验证工具已注册
                        if (!toolContractRegistry.isRegistered(toolCall.getName())) {
                            throw new IllegalArgumentException("未注册的工具: " + toolCall.getName());
                        }
                        
                        // 2. 修复漏洞1：验证工具在本次允许列表内
                        if (!allowedToolNames.contains(toolCall.getName())) {
                            // 拒绝执行未授权工具
                            String errorMsg = String.format(
                                "工具 '%s' 不在本次任务允许的工具列表内，已拒绝执行",
                                toolCall.getName()
                            );
                            
                            // 记录安全审计
                            auditService.log(task.getId(), "TOOL_REJECTED", toolCall.getName(), 
                                errorMsg, "安全拦截：未授权工具调用");
                            
                            // 返回错误给模型
                            String toolResult = ToolResultJson.error(
                                "unauthorized_tool",
                                "该工具不在当前任务的授权范围内",
                                false
                            );
                            messages.add(new LlmClient.Message("tool", toolResult, toolCall.getId()));
                            continue; // 跳过此工具，继续下一个
                        }
                        
                        String toolResult;
                        String normalizedPayload = null;
                        boolean toolSucceeded = false;
                        try {
                            // 执行工具
                            toolResult = toolExecutor.executeTool(
                                userId,
                                allowedToolNames,
                                toolCall.getName(),
                                toolCall.getArguments()
                            );
                            toolSucceeded = true;
                            
                            // 如果是draft工具，尝试提取规范化后的JSON
                            if (toolCall.getName().endsWith("_draft")) {
                                try {
                                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                    com.fasterxml.jackson.databind.JsonNode resultNode = mapper.readTree(toolResult);
                                    if (resultNode.has("normalized_draft_json")) {
                                        normalizedPayload = resultNode.get("normalized_draft_json").asText();
                                    }
                                } catch (Exception e) {
                                    // 如果提取失败，使用原始参数
                                    normalizedPayload = toolCall.getArguments();
                                }
                            }
                        } catch (AgentToolExecutorV2.ToolExecutionException e) {
                            toolResult = ToolResultJson.error(
                                e.getCode(),
                                e.getMessage(),
                                e.isRetryable()
                            );
                        }
                        
                        // 仅成功生成的草案才能进入待确认状态；同一响应在循环结束后批量提交。
                        if (toolSucceeded && toolCall.getName().endsWith("_draft")) {
                            successfulDrafts.add(new DraftPayload(
                                toolCall.getName(),
                                normalizedPayload != null ? normalizedPayload : toolCall.getArguments()
                            ));
                        }
                        
                        // 记录审计日志
                        auditService.logToolCall(
                            task.getId(),
                            toolCall.getName(),
                            toolCall.getArguments(),
                            toolResult
                        );
                        
                        // 将工具结果添加到消息列表
                        messages.add(new LlmClient.Message("tool", toolResult, toolCall.getId()));
                    }

                    createPendingActions(task.getId(), successfulDrafts);
                    
                    // 继续下一轮对话
                    continue;
                }
                
                // 只有模型明确正常停止时，文本才可作为最终回复。
                if (!"stop".equals(llmResponse.getFinishReason())) {
                    throw new IllegalStateException(
                        "LLM未正常完成回复，终止原因: " + llmResponse.getFinishReason()
                    );
                }
                if (llmResponse.getContent() != null && !llmResponse.getContent().isBlank()) {
                    // 重新查询任务状态（因为createPendingAction可能已经修改了状态）
                    AgentTask currentTask = taskMapper.getById(task.getId());
                    
                    // 如果任务状态还不是WAITING_CONFIRMATION（说明没有待确认操作），才更新为RESPONDING
                    // 如果已经是WAITING_CONFIRMATION，保持该状态不变
                    if (!"WAITING_CONFIRMATION".equals(currentTask.getStatus())) {
                        taskMapper.updateStatus(task.getId(), "RESPONDING", null);
                    }
                    auditService.log(task.getId(), "RESPONDING", null, "组织最终回复", null);
                    
                    finalResponse = llmResponse.getContent();
                    break;
                }
                
                // 如果既没有工具调用也没有内容，异常退出
                throw new RuntimeException("LLM返回了无效响应");
            }
            
            // 6. 检查是否达到最大迭代次数
            if (finalResponse == null) {
                throw new RuntimeException("达到最大工具调用次数限制");
            }
            
            // 7. 更新任务完成
            // 注意：如果有待确认操作，任务状态应保持WAITING_CONFIRMATION，不能设为SUCCEEDED
            // 检查是否有待确认操作
            List<AgentAction> pendingActions = actionMapper.getPendingActions(task.getId(), System.currentTimeMillis());
            
            // 草案链路必须保持原文，避免知识旁路改变确认语义。
            if (pendingActions.isEmpty()) {
                finalResponse = enrichWithKnowledge(task.getId(), request.getContent(), finalResponse);
            }

            if (!pendingActions.isEmpty()) {
                // 有待确认操作，只更新内容，状态已经在createPendingAction中设为WAITING_CONFIRMATION
                taskMapper.updateAssistantContent(task.getId(), finalResponse);
            } else {
                // 没有待确认操作，正常完成
                taskMapper.updateContentAndStatus(
                    task.getId(),
                    finalResponse,
                    "SUCCEEDED",
                    System.currentTimeMillis()
                );
            }
            
            auditService.logResponding(task.getId());
            
            // 8. 构建响应
            return buildTaskResponse(task.getId(), userId);
            
        } catch (Exception e) {
            // 任务失败
            taskMapper.updateFailure(
                task.getId(),
                e.getMessage(),
                System.currentTimeMillis()
            );
            
            TaskResponse response = new TaskResponse();
            response.setTaskId(task.getId());
            response.setStatus("FAILED");
            response.setAssistantMessage(null);
            response.setSteps(new ArrayList<>());
            response.setPendingAction(null);
            response.setFailureReason(e.getMessage());
            return response;
        }
    }
    
    /**
     * 构建任务响应
     */
    private TaskResponse buildTaskResponse(Long taskId, Long userId) {
        // 查询任务
        AgentTask task = taskMapper.getByIdAndUserId(taskId, userId);
        if (task == null) {
            return null;
        }
        
        // 查询审计日志，构建步骤列表
        List<AgentAuditLog> auditLogs = auditLogMapper.getByTaskId(taskId);
        List<TaskResponse.TaskStep> steps = new ArrayList<>();
        
        for (AgentAuditLog log : auditLogs) {
            TaskResponse.TaskStep step = new TaskResponse.TaskStep();
            step.setType(log.getStepType());
            step.setToolName(log.getToolName());
            step.setLabel(buildStepLabel(log));
            steps.add(step);
        }
        
        // 动作是确认与恢复的最小单位。待确认和本地写入中的动作都必须完整返回。
        List<AgentAction> activeActions = actionMapper.getByTaskId(taskId);
        List<TaskResponse.PendingAction> pendingActions = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        for (AgentAction action : activeActions) {
            boolean waitingForConfirmation = "WAITING_CONFIRMATION".equals(action.getStatus())
                && action.getExpiresAt() > currentTime;
            boolean waitingForLocalWrite = "LOCAL_WRITE_PENDING".equals(action.getStatus());
            if (waitingForConfirmation || waitingForLocalWrite) {
                pendingActions.add(toPendingAction(action));
            }
        }
        
        TaskResponse response = new TaskResponse();
        response.setTaskId(task.getId());
        response.setStatus(task.getStatus());
        response.setAssistantMessage(task.getAssistantContent());
        response.setSteps(steps);
        response.setPendingActions(pendingActions);
        // 保留首项供尚未升级的客户端读取；新客户端必须使用 pendingActions。
        response.setPendingAction(pendingActions.isEmpty() ? null : pendingActions.get(0));
        response.setFailureReason(task.getFailureReason());
        return response;
    }
    
    private TaskResponse.PendingAction toPendingAction(AgentAction action) {
        TaskResponse.PendingAction pendingAction = new TaskResponse.PendingAction();
        pendingAction.setActionId(action.getId());
        pendingAction.setType(action.getType());
        pendingAction.setStatus(action.getStatus());
        pendingAction.setExpiresAt(action.getExpiresAt());
        pendingAction.setPayload(action.getPayloadJson());
        try {
            pendingAction.setPreview(generateActionPreview(action));
        } catch (Exception e) {
            pendingAction.setPreview("待确认操作");
        }
        return pendingAction;
    }

    /**
     * 将同一任务的动作迁移和聚合状态更新串行化。
     */
    private void lockTask(Long taskId) {
        if (taskMapper.getByIdForUpdate(taskId) == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
    }

    /**
     * 在任务行锁保护下，根据全部动作状态刷新任务聚合状态。
     * 调用方必须先取得同一任务的锁，避免以过期快照覆盖并发迁移结果。
     */
    private void refreshTaskStatus(Long taskId) {
        lockTask(taskId);
        List<AgentAction> actions = actionMapper.getByTaskId(taskId);
        boolean hasLocalWritePending = actions.stream()
            .anyMatch(action -> "LOCAL_WRITE_PENDING".equals(action.getStatus()));
        boolean hasWaitingConfirmation = actions.stream()
            .anyMatch(action -> "WAITING_CONFIRMATION".equals(action.getStatus()));
        boolean hasSucceeded = actions.stream()
            .anyMatch(action -> "SUCCEEDED".equals(action.getStatus()));
        boolean hasFailed = actions.stream()
            .anyMatch(action -> "FAILED".equals(action.getStatus()));
        String status = hasLocalWritePending ? "LOCAL_WRITE_PENDING"
            : hasWaitingConfirmation ? "WAITING_CONFIRMATION"
            : hasFailed ? "FAILED"
            : hasSucceeded ? "SUCCEEDED"
            : "CANCELLED";
        taskMapper.updateStatus(taskId, status,
            ("SUCCEEDED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status))
                ? System.currentTimeMillis()
                : null);
    }
    
    /**
     * 构建步骤标签
     */
    private String buildStepLabel(AgentAuditLog log) {
        return switch (log.getStepType()) {
            case "ANALYZING" -> "正在分析任务";
            case "TOOL_CALL" -> "已查询" + getToolDisplayName(log.getToolName());
            case "RESPONDING" -> "正在生成回复";
            default -> log.getStepType();
        };
    }
    
    /**
     * 获取工具的显示名称
     */
    private String getToolDisplayName(String toolName) {
        if (toolName == null) return "";
        
        return switch (toolName) {
            case "get_training_summary" -> "训练摘要";
            case "get_recent_workouts" -> "最近训练";
            case "get_body_trend" -> "身体数据";
            case "get_today_diet_summary" -> "今日饮食";
            case "get_recovery_status" -> "恢复状态";
            case "get_active_training_plan" -> "训练计划";
            default -> toolName;
        };
    }
    
    /**
     * 查询任务
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return 任务响应
     */
    public TaskResponse getTask(Long userId, Long taskId) {
        return buildTaskResponse(taskId, userId);
    }
    
    /**
     * 确认操作（第一阶段：抢占执行权并返回规范化草案）
     * 
     * 两阶段提交第一阶段：
     * 1. 校验用户权限和任务状态
     * 2. 原子抢占task执行权（WAITING_CONFIRMATION → LOCAL_WRITE_PENDING）
     * 3. 原子抢占action执行权（WAITING_CONFIRMATION → LOCAL_WRITE_PENDING）
     * 4. 返回规范化的草案数据给客户端
     * 5. 客户端收到响应后执行本地写入
     * 6. 写入成功后调用 completeLocalWrite 完成第二阶段
     * 
     * 修复Bug 2：通过task先抢占执行权，确保confirm与cancel互斥
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param actionId 操作ID
     * @return 包含规范化草案的响应
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskResponse confirmAction(Long userId, Long taskId, Long actionId) {
        // 1. 验证任务属于该用户
        AgentTask task = taskMapper.getByIdAndUserId(taskId, userId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在或无权访问");
        }
        // 所有动作迁移与聚合均遵循同一任务锁顺序。
        lockTask(taskId);

        // 2. 验证操作属于该任务
        AgentAction action = actionMapper.getById(actionId);
        if (action == null || !action.getTaskId().equals(taskId)) {
            throw new IllegalArgumentException("操作不存在或不属于该任务");
        }
        
        // Task 状态只反映聚合进度，不能作为某一个 action 的独占锁。
        int actionUpdated = actionMapper.atomicStatusTransition(
            actionId,
            "WAITING_CONFIRMATION",
            "LOCAL_WRITE_PENDING",
            System.currentTimeMillis()
        );
        
        if (actionUpdated == 0) {
            AgentAction currentAction = actionMapper.getById(actionId);
            if (currentAction != null && "SUCCEEDED".equals(currentAction.getStatus())) {
                throw new IllegalStateException("操作已执行，请勿重复确认");
            } else if (currentAction != null && "LOCAL_WRITE_PENDING".equals(currentAction.getStatus())) {
                throw new IllegalStateException("操作正在等待本地写入完成");
            } else if (currentAction != null && "CANCELLED".equals(currentAction.getStatus())) {
                throw new IllegalStateException("操作已取消，无法确认");
            } else if (currentAction != null && "REPLACED".equals(currentAction.getStatus())) {
                throw new IllegalStateException("操作已被新草案替换，无法确认");
            } else if (currentAction != null && currentAction.getExpiresAt() <= System.currentTimeMillis()) {
                actionMapper.atomicStatusTransitionNoExpiry(
                    actionId,
                    "WAITING_CONFIRMATION",
                    "EXPIRED",
                    System.currentTimeMillis()
                );
                refreshTaskStatus(taskId);
                throw new IllegalArgumentException("操作已过期");
            } else {
                throw new IllegalArgumentException("操作状态错误");
            }
        }
        refreshTaskStatus(taskId);
        
        // 5. 不执行写入操作，只返回规范化的草案
        // 客户端拿到规范化草案后执行本地写入，然后调用 completeLocalWrite
        
        // 6. 审计日志
        auditService.log(taskId, "ACTION_CONFIRMED", null, 
            "操作类型: " + action.getType(), "等待本地写入");
        
        // 7. 返回响应（状态仍为 LOCAL_WRITE_PENDING，等待客户端完成）
        return buildTaskResponse(taskId, userId);
    }
    
    /**
     * 完成本地写入（第二阶段：确认本地写入成功）
     * 
     * 幂等设计：
     * - 如果 action 已经是 SUCCEEDED 且 local_reference 匹配，返回成功
     * - 如果 action 是 LOCAL_WRITE_PENDING，执行状态转换
     * - 其他状态视为冲突
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param actionId 操作ID
     * @param localReference 本地保存的记录ID
     * @return 任务响应
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskResponse completeLocalWrite(Long userId, Long taskId, Long actionId, String localReference) {
        // 1. 验证任务属于该用户
        AgentTask task = taskMapper.getByIdAndUserId(taskId, userId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在或无权访问");
        }
        // 与确认、取消和草案替换使用相同的任务锁序列。
        lockTask(taskId);

        // 2. 验证操作属于该任务
        AgentAction action = actionMapper.getById(actionId);
        if (action == null || !action.getTaskId().equals(taskId)) {
            throw new IllegalArgumentException("操作不存在或不属于该任务");
        }
        
        String normalizedLocalReference = normalizeLocalReference(action.getType(), localReference);

        // 3. 幂等性检查：如果已经完成，直接返回成功
        if ("SUCCEEDED".equals(action.getStatus())) {
            // 使用规范化引用比较，允许已升级客户端重放旧格式回执。
            if (normalizedLocalReference.equals(normalizeStoredLocalReference(action.getType(), action.getLocalReference()))) {
                // 已完成且引用匹配，幂等返回
                return getTask(userId, taskId);
            } else {
                // 已完成但引用不匹配，可能是重复操作或数据异常
                throw new IllegalStateException(
                    "操作已完成但本地引用不匹配。期望: " + action.getLocalReference() + 
                    ", 实际: " + localReference
                );
            }
        }
        
        // 4. 验证状态为 LOCAL_WRITE_PENDING
        if (!"LOCAL_WRITE_PENDING".equals(action.getStatus())) {
            throw new IllegalStateException("操作状态不正确，当前状态: " + action.getStatus());
        }
        
        // 5. 原子完成：同时更新 status 和 local_reference（修复Bug 1）
        // 使用原子操作避免并发完成回执时 localReference 被覆盖
        int actionUpdated = actionMapper.atomicCompleteLocalWrite(
            actionId,
            normalizedLocalReference,
            System.currentTimeMillis()
        );
        
        if (actionUpdated == 0) {
            // 状态已被其他请求修改，重新读取并检查幂等性
            AgentAction currentAction = actionMapper.getById(actionId);
            if ("SUCCEEDED".equals(currentAction.getStatus()) && 
                normalizedLocalReference.equals(normalizeStoredLocalReference(
                    currentAction.getType(), currentAction.getLocalReference()
                ))) {
                // 并发请求已完成，幂等返回
                return getTask(userId, taskId);
            }
            throw new IllegalStateException("操作状态已变更，无法完成");
        }
        
        // 6. 任务状态由所有 action 聚合得到；其余待确认动作不受影响。
        refreshTaskStatus(taskId);
        
        // 7. 审计日志
        auditService.log(taskId, "ACTION_COMPLETED", null, 
            "操作类型: " + action.getType(), "本地写入完成，localRef: " + localReference);
        
        // 8. 返回结果
        return getTask(userId, taskId);
    }

    private String normalizeStoredLocalReference(String actionType, String localReference) {
        try {
            return normalizeLocalReference(actionType, localReference);
        } catch (IllegalArgumentException ignored) {
            return localReference;
        }
    }

    /**
     * 将新版 JSON 与历史字符串引用收敛为同一规范表示，供存储与幂等比较使用。
     */
    private String normalizeLocalReference(String actionType, String localReference) {
        if (localReference == null || localReference.isBlank()) {
            throw new IllegalArgumentException("本地引用不能为空");
        }
        String expectedType = switch (actionType) {
            case "CREATE_TRAINING_PLAN" -> "training_plan";
            case "CREATE_DIET_RECORD" -> "diet_records";
            default -> throw new IllegalArgumentException("不支持本地写入的操作类型: " + actionType);
        };

        try {
            JsonNode root = LOCAL_REFERENCE_MAPPER.readTree(localReference);
            if (root != null && root.isObject()) {
                JsonNode type = root.get("type");
                JsonNode ids = root.get("ids");
                if (root.size() != 2 || type == null || !type.isTextual() || !expectedType.equals(type.textValue())
                    || ids == null || !ids.isArray() || ids.isEmpty()
                    || ("training_plan".equals(expectedType) && ids.size() != 1)) {
                    throw new IllegalArgumentException("本地引用格式或类型不匹配");
                }
                List<Long> normalizedIds = new ArrayList<>();
                Set<Long> uniqueIds = new HashSet<>();
                for (JsonNode id : ids) {
                    if (!id.isIntegralNumber() || !id.canConvertToLong() || id.asLong() <= 0
                        || !uniqueIds.add(id.asLong())) {
                        throw new IllegalArgumentException("本地引用包含无效或重复记录ID");
                    }
                    normalizedIds.add(id.asLong());
                }
                return canonicalLocalReference(expectedType, normalizedIds);
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
            // 兼容已发布客户端的历史字符串格式，统一为当前 JSON 表示。
        }

        String[] rawIds = localReference.split(",", -1);
        if (rawIds.length == 0 || ("training_plan".equals(expectedType) && rawIds.length != 1)) {
            throw new IllegalArgumentException("历史本地引用格式不匹配");
        }
        List<Long> ids = new ArrayList<>();
        Set<Long> uniqueIds = new HashSet<>();
        try {
            for (String rawId : rawIds) {
                long id = Long.parseLong(rawId);
                if (id <= 0 || !uniqueIds.add(id)) {
                    throw new IllegalArgumentException("本地引用包含无效或重复记录ID");
                }
                ids.add(id);
            }
            return canonicalLocalReference(expectedType, ids);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("本地引用格式不正确", e);
        }
    }

    private String canonicalLocalReference(String type, List<Long> ids) {
        try {
            return LOCAL_REFERENCE_MAPPER.writeValueAsString(java.util.Map.of("type", type, "ids", ids));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("无法序列化本地引用", e);
        }
    }
    
    /**
     * 查询用户所有待完成本地写入的操作
     * 用于客户端启动时恢复未完成的操作
     * 
     * @param userId 用户ID
     * @return 待完成操作列表
     */
    public List<com.fitness.server.dto.agent.PendingLocalWriteResponse> getPendingLocalWrites(Long userId) {
        List<AgentAction> pendingActions = actionMapper.getPendingLocalWrites(userId);
        List<com.fitness.server.dto.agent.PendingLocalWriteResponse> responses = new ArrayList<>();
        
        for (AgentAction action : pendingActions) {
            com.fitness.server.dto.agent.PendingLocalWriteResponse response = 
                new com.fitness.server.dto.agent.PendingLocalWriteResponse(
                    action.getTaskId(),
                    action.getId(),
                    action.getType(),
                    action.getPayloadJson(),
                    action.getCreatedAt()
                );
            responses.add(response);
        }
        
        return responses;
    }
    
    /**
     * 创建待确认操作。每个草案保持独立的动作和幂等键。
     * 同一任务、同一类型只保留最新的待确认草案。
     */
    private void createPendingAction(Long taskId, String toolName, String argumentsJson) {
        createPendingActions(taskId, List.of(new DraftPayload(toolName, argumentsJson)));
    }

    /**
     * 同一轮生成的草案必须作为一个原子批次落库，避免部分成功后任务被标记失败。
     */
    private void createPendingActions(Long taskId, List<DraftPayload> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            return;
        }

        List<DraftPayload> normalizedDrafts = new ArrayList<>();
        Set<String> actionTypes = new HashSet<>();
        for (DraftPayload draft : drafts) {
            String actionType = actionTypeForTool(draft.toolName);
            if (!actionTypes.add(actionType)) {
                throw new IllegalArgumentException("同一任务批次包含重复草案类型: " + actionType);
            }
            normalizedDrafts.add(new DraftPayload(
                draft.toolName,
                validateDraftPayload(draft.toolName, draft.payloadJson)
            ));
        }

        long now = System.currentTimeMillis();
        transactionTemplate.executeWithoutResult(status -> {
            lockTask(taskId);
            for (DraftPayload draft : normalizedDrafts) {
                String actionType = actionTypeForTool(draft.toolName);
                actionMapper.replaceWaitingActionsByType(taskId, actionType, now);

                AgentAction action = new AgentAction();
                action.setTaskId(taskId);
                action.setType(actionType);
                action.setPayloadJson(draft.payloadJson);
                action.setStatus("WAITING_CONFIRMATION");
                action.setIdempotencyKey(java.util.UUID.randomUUID().toString());
                action.setCreatedAt(now);
                action.setExpiresAt(now + 5 * 60 * 1000);
                actionMapper.insert(action);
            }
            refreshTaskStatus(taskId);
        });
    }
    
    /**
     * 草案创建和编辑都必须经过工具契约的同一校验边界。
     */
    private String actionTypeForTool(String toolName) {
        return switch (toolName) {
            case "create_training_plan_draft" -> "CREATE_TRAINING_PLAN";
            case "create_diet_record_draft" -> "CREATE_DIET_RECORD";
            default -> throw new IllegalArgumentException("不支持的草案工具: " + toolName);
        };
    }

    private String validateDraftPayload(String toolName, String payloadJson) {
        ToolContract contract = toolContractRegistry.getContract(toolName);
        if (contract == null || contract.getCategory() != ToolContract.ToolCategory.DRAFT) {
            throw new IllegalArgumentException("未注册的草案工具: " + toolName);
        }
        try {
            Map<String, Object> normalizedArguments = contract.validateAndNormalize(payloadJson);
            Object rawPayload = normalizedArguments.get("_raw");
            if (!(rawPayload instanceof String normalizedPayload) || normalizedPayload.isBlank()) {
                throw new IllegalStateException("草案校验未返回可持久化的规范化内容");
            }
            return normalizedPayload;
        } catch (ToolContract.ValidationException e) {
            throw new IllegalArgumentException("草案格式不正确: " + e.getField() + " - " + e.getMessage(), e);
        }
    }

    private String executeWriteAction(Long userId, AgentAction action) throws Exception {
        return switch (action.getType()) {
            case "CREATE_TRAINING_PLAN" -> createTrainingPlan(userId, action.getPayloadJson());
            case "CREATE_DIET_RECORD" -> createDietRecord(userId, action.getPayloadJson());
            default -> throw new IllegalArgumentException("未知操作类型: " + action.getType());
        };
    }
    
    private String createTrainingPlan(Long userId, String payloadJson) throws Exception {
        // 训练计划由Android端本地保存，不需要服务端写入
        // Android直接保存本地后移除待确认卡片，不调用confirmAction接口
        // 用户通过sync接口上传后才会保存到云端
        
        // 这个方法实际不会被调用（Android不会对训练计划调用confirmAction）
        // 如果被调用了，说明客户端逻辑有误
        throw new UnsupportedOperationException(
            "训练计划应由Android端直接保存到本地，不应调用confirmAction接口"
        );
        
        /* 如果未来需要服务端直接创建，使用以下代码：
        
        // 解析草案
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fitness.server.agent.dto.TrainingPlanDraftDto draft = 
            mapper.readValue(payloadJson, com.fitness.server.agent.dto.TrainingPlanDraftDto.class);
        
        // 创建训练计划实体
        TrainingPlan plan = new TrainingPlan();
        plan.setUserId(userId);
        plan.setTitle(draft.getTitle());
        plan.setDescription(draft.getDescription());
        plan.setGoal(draft.getGoal());
        plan.setExperience(draft.getExperience());
        plan.setTargetMuscles(draft.getTargetMuscles());
        plan.setTrainingDays(draft.getTrainingDays());
        plan.setTrainingDuration(draft.getTrainingDuration());
        plan.setEquipment(draft.getEquipment());
        plan.setDetails(mapper.writeValueAsString(draft.getDays()));
        plan.setIsPinned(false);
        plan.setIsFromRecommendation(true);
        plan.setCreatedAt(System.currentTimeMillis());
        plan.setUpdatedAt(System.currentTimeMillis());
        
        trainingPlanMapper.insert(plan);
        
        return "{\"success\": true, \"planId\": " + plan.getId() + ", \"message\": \"训练计划已创建\"}";
        */
    }
    
    private String createDietRecord(Long userId, String payloadJson) throws Exception {
        // 饮食记录由Android端本地保存，不需要服务端写入
        // Android直接保存本地后移除待确认卡片，不调用confirmAction接口
        // 用户通过sync接口上传后才会保存到云端
        
        // 这个方法实际不会被调用（Android不会对饮食记录调用confirmAction）
        // 如果被调用了，说明客户端逻辑有误
        throw new UnsupportedOperationException(
            "饮食记录应由Android端直接保存到本地，不应调用confirmAction接口"
        );
        
        /* 如果未来需要服务端直接创建，使用以下代码：
        
        // 解析草案
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fitness.server.agent.dto.DietRecordDraftDto draft = 
            mapper.readValue(payloadJson, com.fitness.server.agent.dto.DietRecordDraftDto.class);
        
        // 解析日期（格式：yyyy-MM-dd）
        long date;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            date = sdf.parse(draft.getDate()).getTime();
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式错误，应为yyyy-MM-dd: " + draft.getDate());
        }
        
        int createdCount = 0;
        
        // 批量创建饮食记录
        for (com.fitness.server.agent.dto.DietRecordDraftDto.RecordItem item : draft.getRecords()) {
            com.fitness.server.entity.DietRecord record = new com.fitness.server.entity.DietRecord();
            record.setUserId(userId);
            record.setDate(date);
            record.setMealType(item.getMealType());
            record.setFoodName(item.getFoodName());
            record.setCalories(item.getCalories());
            record.setProtein(item.getProtein());
            record.setCarbs(item.getCarbs());
            record.setFat(item.getFat());
            record.setAmount(item.getAmount());
            record.setCreatedAt(System.currentTimeMillis());
            record.setUpdatedAt(System.currentTimeMillis());
            
            dietRecordMapper.insert(record);
            createdCount++;
        }
        
        return String.format(
            "{\"success\": true, \"createdCount\": %d, \"date\": \"%s\", \"message\": \"已保存%d条饮食记录\"}",
            createdCount, draft.getDate(), createdCount
        );
        */
    }
    
    /**
     * 生成待确认操作的预览文本
     */
    private Object generateActionPreview(AgentAction action) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        
        return switch (action.getType()) {
            case "CREATE_TRAINING_PLAN" -> {
                com.fitness.server.agent.dto.TrainingPlanDraftDto draft = 
                    mapper.readValue(action.getPayloadJson(), com.fitness.server.agent.dto.TrainingPlanDraftDto.class);
                
                int totalExercises = draft.getDays().stream()
                    .mapToInt(day -> day.getExercises() != null ? day.getExercises().size() : 0)
                    .sum();
                
                yield String.format("【%s】\n目标：%s\n训练天数：%d天/周\n动作总数：%d个",
                    draft.getTitle(),
                    draft.getGoal(),
                    draft.getTrainingDays(),
                    totalExercises
                );
            }
            case "CREATE_DIET_RECORD" -> {
                com.fitness.server.agent.dto.DietRecordDraftDto draft = 
                    mapper.readValue(action.getPayloadJson(), com.fitness.server.agent.dto.DietRecordDraftDto.class);
                
                // 计算汇总数据
                int totalCalories = draft.getRecords().stream()
                    .mapToInt(r -> r.getCalories() != null ? r.getCalories() : 0)
                    .sum();
                
                float totalProtein = (float) draft.getRecords().stream()
                    .mapToDouble(r -> r.getProtein() != null ? r.getProtein() : 0.0)
                    .sum();
                
                float totalCarbs = (float) draft.getRecords().stream()
                    .mapToDouble(r -> r.getCarbs() != null ? r.getCarbs() : 0.0)
                    .sum();
                
                float totalFat = (float) draft.getRecords().stream()
                    .mapToDouble(r -> r.getFat() != null ? r.getFat() : 0.0)
                    .sum();
                
                // 构建详细的 Map 格式 preview
                java.util.Map<String, Object> previewMap = new java.util.HashMap<>();
                previewMap.put("title", "饮食记录待确认");
                previewMap.put("date", draft.getDate());
                previewMap.put("recordCount", draft.getRecords().size());
                previewMap.put("totalCalories", totalCalories);
                previewMap.put("totalProtein", totalProtein);
                previewMap.put("totalCarbs", totalCarbs);
                previewMap.put("totalFat", totalFat);
                
                // 详细食物列表
                java.util.List<java.util.Map<String, Object>> detailsList = draft.getRecords().stream()
                    .map(r -> {
                        java.util.Map<String, Object> item = new java.util.HashMap<>();
                        item.put("mealType", r.getMealType());
                        item.put("foodName", r.getFoodName());
                        item.put("amount", r.getAmount());
                        item.put("calories", r.getCalories() != null ? r.getCalories() : 0);
                        item.put("protein", r.getProtein() != null ? r.getProtein() : 0f);
                        item.put("carbs", r.getCarbs() != null ? r.getCarbs() : 0f);
                        item.put("fat", r.getFat() != null ? r.getFat() : 0f);
                        item.put("isEstimated", r.isEstimated());
                        return item;
                    })
                    .collect(java.util.stream.Collectors.toList());
                
                previewMap.put("details", detailsList);
                
                yield previewMap;
            }
            default -> "待确认操作";
        };
    }
    
    /**
     * 根据任务类型筛选可用工具
     * 
     * 修复：精确识别意图，遵循最小权限原则
     * - 查询/分析 → READ
     * - 训练计划创建 → READ + create_training_plan_draft
     * - 饮食记录创建 → READ + create_diet_record_draft
     * - 意图不明确 → READ（让模型追问）
     * 
     * @param userMessage 用户消息
     * @return 允许的工具列表
     */
    private List<LlmClient.Tool> determineAllowedTools(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return authorizedToolResolver.resolve(List.of("get_user_fitness_profile"));
        }

        String lowerMessage = userMessage.toLowerCase();
        Set<String> allowedToolNames = new java.util.LinkedHashSet<>();

        if (containsTrainingDomain(lowerMessage)) {
            allowedToolNames.addAll(List.of(
                "get_training_summary",
                "get_recent_workouts",
                "get_recovery_status",
                "get_active_training_plan",
                "get_training_schedule",
                "get_training_progress"
            ));
        }
        if (containsNutritionDomain(lowerMessage)) {
            allowedToolNames.add("get_diet_summary");
            if (!requiresDateSpecificDietSummary(lowerMessage)) {
                allowedToolNames.add("get_today_diet_summary");
            }
            allowedToolNames.addAll(List.of(
                "get_daily_nutrition_progress",
                "get_user_fitness_profile"
            ));
        }
        if (containsProgressDomain(lowerMessage)) {
            allowedToolNames.addAll(List.of(
                "get_body_trend",
                "get_training_progress",
                "get_diet_summary",
                "get_daily_nutrition_progress",
                "get_user_fitness_profile"
            ));
        }
        if (containsTrainingPlanCreationIntent(lowerMessage)) {
            allowedToolNames.add("create_training_plan_draft");
        }
        if (containsDietRecordCreationIntent(lowerMessage)) {
            allowedToolNames.add("create_diet_record_draft");
        }

        // 意图无法识别时只允许读取档案，避免旧降级链路默认跨领域读取全部数据。
        if (allowedToolNames.isEmpty()) {
            allowedToolNames.add("get_user_fitness_profile");
        }
        return authorizedToolResolver.resolve(new ArrayList<>(allowedToolNames));
    }

    private boolean containsTrainingDomain(String message) {
        return message.contains("训练") || message.contains("健身") || message.contains("锻炼") ||
            message.contains("力量") || message.contains("动作") || message.contains("计划") ||
            message.contains("练什么") || message.contains("练哪") ||
            message.contains("做什么训练") || message.contains("安排什么训练");
    }

    private boolean containsNutritionDomain(String message) {
        return message.contains("饮食") || message.contains("吃") || message.contains("喝") ||
            message.contains("食物") || message.contains("热量") || message.contains("蛋白") ||
            message.contains("碳水") || message.contains("脂肪") || message.contains("早餐") ||
            message.contains("午餐") || message.contains("晚餐") || message.contains("加餐");
    }

    private boolean requiresDateSpecificDietSummary(String message) {
        return message.contains("昨天") || message.contains("前天") || message.contains("上周") ||
            message.contains("最近") || message.matches(".*\\d{4}[-/.年]\\d{1,2}[-/.月]\\d{1,2}.*");
    }

    private boolean containsProgressDomain(String message) {
        return message.contains("体重") || message.contains("体脂") || message.contains("肌肉") ||
            message.contains("趋势") || message.contains("进度") || message.contains("完成率") ||
            message.contains("连续");
    }
    
    /**
     * 判断本轮是否属于训练计划的创建或资料收集。
     *
     * 计划资料通常跨多轮提供，例如“增肌，新手，无伤病”。这类输入必须让模型
     * 看见草案工具，才能在资料补全后于同一会话中创建草案；资料尚缺时由提示词要求追问。
     */
    private boolean containsTrainingPlanCreationIntent(String lowerMessage) {
        boolean hasExplicitCreation = (lowerMessage.contains("训练") ||
            lowerMessage.contains("健身") || lowerMessage.contains("锻炼")) &&
            (lowerMessage.contains("计划") || lowerMessage.contains("方案") ||
                lowerMessage.contains("制定") || lowerMessage.contains("生成") ||
                lowerMessage.contains("创建") || lowerMessage.contains("设计") ||
                lowerMessage.contains("做一个") || lowerMessage.contains("帮我做") ||
                lowerMessage.contains("给我做"));

        boolean hasGoal = lowerMessage.contains("增肌") || lowerMessage.contains("增重") ||
            lowerMessage.contains("减脂") || lowerMessage.contains("减肥") ||
            lowerMessage.contains("塑形") || lowerMessage.contains("力量") ||
            lowerMessage.contains("体能");
        boolean hasExperience = lowerMessage.contains("新手") || lowerMessage.contains("初学") ||
            lowerMessage.contains("中级") || lowerMessage.contains("高级") ||
            lowerMessage.contains("有经验");
        boolean hasRestriction = lowerMessage.contains("无伤病") || lowerMessage.contains("没受伤") ||
            lowerMessage.contains("伤病") || lowerMessage.contains("受伤") ||
            lowerMessage.contains("疼痛") || lowerMessage.contains("旧伤");
        boolean hasScheduleOrEquipment = lowerMessage.contains("每周") || lowerMessage.contains("天练") ||
            lowerMessage.contains("器械") || lowerMessage.contains("哑铃") ||
            lowerMessage.contains("杠铃") || lowerMessage.contains("健身房") ||
            lowerMessage.contains("徒手");

        return hasExplicitCreation || hasGoal || hasExperience || hasRestriction || hasScheduleOrEquipment;
    }
    
    /**
     * 检测饮食记录创建意图。摄入汇总等查询只允许读取工具。
     */
    private boolean containsDietRecordCreationIntent(String lowerMessage) {
        if (containsDietQueryIntent(lowerMessage)) {
            return false;
        }

        boolean hasExplicitConsumption = hasExplicitConsumptionWithFood(lowerMessage);
        boolean hasDietContext = lowerMessage.contains("饮食") ||
                                 lowerMessage.contains("吃") || lowerMessage.contains("喝") ||
                                 lowerMessage.contains("食物") || lowerMessage.contains("餐") ||
                                 lowerMessage.contains("早餐") || lowerMessage.contains("午餐") ||
                                 lowerMessage.contains("晚餐") || lowerMessage.contains("加餐");
        boolean hasRecordAction = lowerMessage.contains("记录") ||
                                  lowerMessage.contains("添加") || lowerMessage.contains("保存") ||
                                  lowerMessage.contains("记一下") || lowerMessage.contains("记个") ||
                                  lowerMessage.contains("帮我记") || lowerMessage.contains("给我记") ||
                                  lowerMessage.contains("生成") || lowerMessage.contains("创建") ||
                                  lowerMessage.contains("草案");

        return hasExplicitConsumption || (hasDietContext && hasRecordAction);
    }

    private boolean containsDietQueryIntent(String lowerMessage) {
        boolean asksConsumedItems = lowerMessage.contains("吃了什么") || lowerMessage.contains("喝了什么") ||
            lowerMessage.contains("吃的什么") || lowerMessage.contains("喝的什么");
        boolean asksNutritionTotal = lowerMessage.contains("多少热量") || lowerMessage.contains("热量多少") ||
            lowerMessage.contains("摄入多少") || lowerMessage.contains("摄入了多少") || lowerMessage.contains("吃了多少");
        return asksConsumedItems || asksNutritionTotal || lowerMessage.contains("饮食情况") ||
            lowerMessage.contains("饮食汇总") || lowerMessage.contains("营养摄入") || lowerMessage.contains("营养情况");
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
     * 取消任务中全部尚未确认的草案。
     * 已进入本地写入的动作不受影响，任务状态始终由全部动作重新聚合。
     *
     * @param userId 用户ID
     * @param taskId 任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long userId, Long taskId) {
        AgentTask task = taskMapper.getByIdAndUserId(taskId, userId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在或无权访问");
        }

        lockTask(taskId);
        long now = System.currentTimeMillis();
        for (AgentAction action : actionMapper.getWaitingActions(taskId)) {
            actionMapper.atomicStatusTransitionNoExpiry(
                action.getId(), "WAITING_CONFIRMATION", "CANCELLED", now
            );
        }
        refreshTaskStatus(taskId);
    }
    
    /**
     * 取消单个待确认动作，避免影响同一任务的其他草案。
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskResponse cancelAction(Long userId, Long taskId, Long actionId) {
        AgentTask task = taskMapper.getByIdAndUserId(taskId, userId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在或无权访问");
        }
        // 与确认、完成和草案替换使用相同的任务锁序列。
        lockTask(taskId);

        AgentAction action = actionMapper.getById(actionId);
        if (action == null || !action.getTaskId().equals(taskId)) {
            throw new IllegalArgumentException("操作不存在或不属于该任务");
        }
        if ("CANCELLED".equals(action.getStatus())) {
            return getTask(userId, taskId);
        }
        if (!"WAITING_CONFIRMATION".equals(action.getStatus())) {
            throw new IllegalStateException("操作已确认或正在写入，无法取消");
        }
        if (actionMapper.atomicStatusTransitionNoExpiry(
            actionId, "WAITING_CONFIRMATION", "CANCELLED", System.currentTimeMillis()) == 0) {
            throw new IllegalStateException("操作状态已变更，无法取消");
        }
        refreshTaskStatus(taskId);
        auditService.log(taskId, "ACTION_CANCELLED", null, "操作类型: " + action.getType(), "用户取消草案");
        return getTask(userId, taskId);
    }

    /**
     * 更新操作草案（编辑功能）
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param actionId 操作ID
     * @param newPayloadJson 新的草案 JSON
     * @return 更新后的任务响应
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskResponse updateActionPayload(Long userId, Long taskId, Long actionId, String newPayloadJson) {
        // 1. 验证任务属于该用户
        AgentTask task = taskMapper.getByIdAndUserId(taskId, userId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在或无权访问");
        }
        
        // 与确认、取消、完成和草案替换使用相同的任务锁序列。
        // 锁必须位于动作状态读取之前，防止已读取 WAITING_CONFIRMATION 的编辑
        // 与并发确认交错，从而覆盖客户端即将执行的草案。
        lockTask(taskId);

        // 2. 验证操作属于该任务
        AgentAction action = actionMapper.getById(actionId);
        if (action == null || !action.getTaskId().equals(taskId)) {
            throw new IllegalArgumentException("操作不存在或不属于该任务");
        }
        
        // 3. 只有 WAITING_CONFIRMATION 状态可以编辑
        if (!"WAITING_CONFIRMATION".equals(action.getStatus())) {
            throw new IllegalStateException("操作已确认或取消，无法编辑（当前状态：" + action.getStatus() + "）");
        }
        
        // 4. 校验新的 payloadJson 格式
        String normalizedPayloadJson = validateDraftPayload(
            toolNameForActionType(action.getType()), newPayloadJson
        );
        
        // 5. 更新 payloadJson
        actionMapper.updatePayloadJson(actionId, normalizedPayloadJson);
        
        // 6. 记录审计日志
        auditService.log(taskId, "PAYLOAD_UPDATED", null, 
            "操作类型: " + action.getType(), "用户编辑了草案");
        
        // 7. 返回更新后的任务响应
        return getTask(userId, taskId);
    }
    
    private String toolNameForActionType(String actionType) {
        return switch (actionType) {
            case "CREATE_TRAINING_PLAN" -> "create_training_plan_draft";
            case "CREATE_DIET_RECORD" -> "create_diet_record_draft";
            default -> throw new IllegalArgumentException("不支持编辑的草案类型: " + actionType);
        };
    }

    /**
     * F1-A 任务2：将SupervisorResult转换为TaskResponse
     * 
     * P2修复：正确处理PARTIAL状态
     * - SUCCESS + 有草案 -> WAITING_CONFIRMATION
     * - SUCCESS + 无草案 -> SUCCEEDED
     * - PARTIAL + 有草案 -> WAITING_CONFIRMATION（部分成功但有可用草案）
     * - PARTIAL + 无草案 -> PARTIAL（不能标记为SUCCEEDED）
     * - FAILED -> FAILED
     * 
     * @param supervisorResult Supervisor执行结果
     * @param userId 用户ID
     * @param request 原始请求
     * @return TaskResponse
     */
    private String enrichWithKnowledge(Long taskId, String userMessage, String originalMessage) {
        try {
            KnowledgeAgent.EnrichmentResult result = knowledgeAgent.enrichGeneralAdviceWithCitations(userMessage, originalMessage);
            if (result.hasKnowledgeHit()) {
                auditService.logKnowledgeHit(taskId, result.citations());
                logger.info("Knowledge sidecar appended {} approved chunk(s) to task {}",
                    result.citations().size(), taskId);
            }
            return result.message();
        } catch (Exception exception) {
            logger.debug("Knowledge sidecar bypassed: {}", exception.getMessage());
            return originalMessage;
        }
    }

    private TaskResponse convertSupervisorResultToTaskResponse(
            com.fitness.server.agent.SupervisorService.SupervisorResult supervisorResult,
            Long userId,
            CreateTaskRequest request) {
        
        // 1. 创建任务记录
        AgentTask task = new AgentTask();
        task.setUserId(userId);
        task.setSessionId(request.getSessionId());
        task.setUserContent(request.getContent());
        task.setStatus("RECEIVED");
        task.setCreatedAt(System.currentTimeMillis());
        taskMapper.insert(task);
        
        try {
            // 2. 更新状态为ANALYZING
            taskMapper.updateStatus(task.getId(), "ANALYZING", null);
            
            // 3. 记录Supervisor审计日志
            if (supervisorResult.getExecutionPlan() != null) {
                auditService.logSupervisorPlanning(
                    task.getId(),
                    String.join(", ", supervisorResult.getExecutionPlan().getDomains()),
                    String.join(", ", supervisorResult.getExecutionPlan().getSelectedAgents())
                );
            }
            
            if (supervisorResult.getSafetyDecision() != null) {
                auditService.logSafetyCheck(
                    task.getId(),
                    supervisorResult.getSafetyDecision().getRiskLevel().toString(),
                    supervisorResult.getSafetyDecision().isBlockDraft()
                );
            }
            
            // 4. 记录Agent执行审计日志，并收集所有draftCandidates（F1-B修复：支持多草案）
            List<DraftCandidate> draftCandidates = new ArrayList<>();
            
            if (supervisorResult.getAgentResults() != null) {
                for (com.fitness.server.agent.dto.AgentResultDto agentResult : supervisorResult.getAgentResults()) {
                    String findingsSummary = agentResult.getFindings() != null && !agentResult.getFindings().isEmpty()
                        ? agentResult.getFindings().get(0).substring(0, Math.min(agentResult.getFindings().get(0).length(), 200))
                        : "无";
                    auditService.logAgentExecution(
                        task.getId(),
                        agentResult.getAgentName(),
                        agentResult.getStatus().toString(),
                        findingsSummary
                    );
                    
                    // F1-B修复：收集所有草案（不覆盖）
                    if (agentResult.getDraftCandidate() != null && !agentResult.getDraftCandidate().isEmpty()) {
                        String toolName = null;
                        // 根据Agent类型确定工具名
                        if ("TrainingAgent".equals(agentResult.getAgentName())) {
                            toolName = "create_training_plan_draft";
                        } else if ("NutritionAgent".equals(agentResult.getAgentName())) {
                            toolName = "create_diet_record_draft";
                        }
                        
                        if (toolName != null) {
                            draftCandidates.add(new DraftCandidate(
                                agentResult.getAgentName(),
                                agentResult.getDraftCandidate(),
                                toolName
                            ));
                            logger.info("Collected draft from {}", agentResult.getAgentName());
                        }
                    }
                }
            }
            
            boolean hasDraftCandidate = !draftCandidates.isEmpty();
            
            // 5. P2修复：根据Supervisor状态和草案存在性决定最终任务状态
            String supervisorStatus = supervisorResult.getStatus();
            
            if ("FAILED".equals(supervisorStatus)) {
                // 全部失败
                taskMapper.updateContentAndStatus(
                    task.getId(),
                    supervisorResult.getFinalMessage(),
                    "FAILED",
                    System.currentTimeMillis()
                );
            } else if (hasDraftCandidate) {
                // 同一 Supervisor 结果中的草案必须全量校验后同事务入库，禁止部分提交。
                List<DraftPayload> payloads = draftCandidates.stream()
                    .map(draft -> new DraftPayload(draft.toolName, draft.draftJson))
                    .toList();
                createPendingActions(task.getId(), payloads);
                for (DraftCandidate draft : draftCandidates) {
                    logger.info("Created pending action for {} with tool {}", draft.agentName, draft.toolName);
                }
                // 有草案（无论是SUCCESS还是PARTIAL）-> WAITING_CONFIRMATION
                taskMapper.updateAssistantContent(task.getId(), supervisorResult.getFinalMessage());
                logger.info("Task {} has {} draft(s), status: WAITING_CONFIRMATION", task.getId(), draftCandidates.size());
            } else if ("SUCCESS".equals(supervisorStatus)) {
                // SUCCESS 且无草案 -> SUCCEEDED
                taskMapper.updateContentAndStatus(
                    task.getId(),
                    enrichWithKnowledge(task.getId(), request.getContent(), supervisorResult.getFinalMessage()),
                    "SUCCEEDED",
                    System.currentTimeMillis()
                );
                logger.info("Task {} succeeded without draft", task.getId());
            } else if ("PARTIAL".equals(supervisorStatus)) {
                // PARTIAL 且无草案 -> PARTIAL（不能标记为SUCCEEDED）
                taskMapper.updateContentAndStatus(
                    task.getId(),
                    enrichWithKnowledge(task.getId(), request.getContent(), supervisorResult.getFinalMessage()),
                    "PARTIAL",
                    System.currentTimeMillis()
                );
                logger.warn("Task {} partial without draft", task.getId());
            } else {
                // 其他状态（BLOCKED等）-> FAILED
                taskMapper.updateContentAndStatus(
                    task.getId(),
                    supervisorResult.getFinalMessage(),
                    "FAILED",
                    System.currentTimeMillis()
                );
            }
            
            // 6. 构建TaskResponse
            return buildTaskResponse(task.getId(), userId);
            
        } catch (Exception e) {
            // 转换失败，记录错误
            org.slf4j.LoggerFactory.getLogger(AgentOrchestrator.class)
                .error("Convert supervisor result failed: {}", e.getMessage(), e);
            
            taskMapper.updateFailure(
                task.getId(),
                "Supervisor结果转换失败: " + e.getMessage(),
                System.currentTimeMillis()
            );
            
            TaskResponse response = new TaskResponse();
            response.setTaskId(task.getId());
            response.setStatus("FAILED");
            response.setFailureReason(e.getMessage());
            return response;
        }
    }
    
    private record DraftPayload(String toolName, String payloadJson) {
    }

    /**
     * F1-B修复：草案候选对象（支持多草案）
     */
    private static class DraftCandidate {
        String agentName;
        String draftJson;
        String toolName;
        
        DraftCandidate(String agentName, String draftJson, String toolName) {
            this.agentName = agentName;
            this.draftJson = draftJson;
            this.toolName = toolName;
        }
    }
}
