package com.fitness.training.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.data.entity.AiChatMessage
import com.fitness.training.network.RetrofitClient
import com.fitness.training.network.dto.CreateAgentTaskRequest
import com.fitness.training.network.dto.ConfirmActionRequest
import com.fitness.training.util.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Agent ViewModel - 管理Agent任务状态
 */
class AgentViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = FitnessDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()
    
    private val sessionId = UUID.randomUUID().toString()
    
    init {
        loadMessagesFromDatabase()
        // 恢复未完成的本地写入操作
        recoverPendingLocalWrites()
    }
    
    /**
     * 从数据库加载历史消息
     */
    private fun loadMessagesFromDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = UserSession.getCurrentUserId(getApplication())
            val dbMessages = database.aiChatMessageDao().getAllMessages(userId)
            
            withContext(Dispatchers.Main) {
                if (dbMessages.isEmpty()) {
                    // 首次打开，显示欢迎消息
                    val welcomeItem = AgentMessageItem.AssistantMessage(
                        content = "👋 你好！我是你的智能健身助手。\n\n你可以问我：\n• 训练动作的正确姿势\n• 制定训练计划\n• 饮食和营养建议\n• 分析你的训练数据\n\n有什么可以帮你的？",
                        taskId = 0L
                    )
                    _uiState.value = _uiState.value.copy(
                        items = listOf(welcomeItem),
                        sessionId = sessionId
                    )
                } else {
                    // 加载历史消息
                    val items = dbMessages.map { dbMsg ->
                        if (dbMsg.isUser) {
                            AgentMessageItem.UserMessage(dbMsg.content)
                        } else {
                            AgentMessageItem.AssistantMessage(
                                content = dbMsg.content,
                                taskId = 0L  // 历史消息没有taskId
                            )
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        items = items,
                        sessionId = sessionId
                    )
                }
            }
        }
    }
    
    /**
     * 发送用户消息，创建Agent任务
     */
    fun sendMessage(content: String) {
        if (_uiState.value.isSubmitting) return

        if (content.isTextConfirmation()) {
            routeTextConfirmation(content)
            return
        }
        
        // 添加用户消息到UI
        val userMessage = AgentMessageItem.UserMessage(content)
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items + userMessage,
            isSubmitting = true,
            error = null
        )
        
        // 保存用户消息到数据库
        saveMessageToDatabase(content, isUser = true)
        
        // 添加加载中指示器
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items + AgentMessageItem.LoadingMessage()
        )
        
        // 调用Agent API
        viewModelScope.launch {
            try {
                val token = "Bearer ${UserSession.getToken(getApplication())}"
                val request = CreateAgentTaskRequest(
                    content = content,
                    sessionId = sessionId
                )
                
                val response = withContext(Dispatchers.IO) {
                    apiService.createAgentTask(token, request)
                }
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val taskResponse = response.body()?.data
                    if (taskResponse != null) {
                        handleTaskResponse(taskResponse)
                    } else {
                        handleError("服务器返回数据为空")
                    }
                } else {
                    handleError("请求失败: ${response.body()?.message ?: response.message()}")
                }
            } catch (e: Exception) {
                handleError("网络错误: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isSubmitting = false)
            }
        }
    }

    private fun String.isTextConfirmation(): Boolean = trim()
        .lowercase()
        .trimEnd('。', '！', '!', '？', '?') in setOf(
            "确认", "确定", "好的", "好", "是", "同意", "提交", "保存"
        )

    private fun routeTextConfirmation(content: String) {
        val pendingItem = _uiState.value.items
            .asReversed()
            .filterIsInstance<AgentMessageItem.PendingActionItem>()
            .firstOrNull { item ->
                item.action.status.isWaitingForConfirmation() &&
                    item.action.expiresAt > System.currentTimeMillis()
            }

        if (pendingItem == null) {
            val message = "当前没有可确认的饮食草案，请先重新生成草案后再确认。"
            _uiState.value = _uiState.value.copy(
                items = _uiState.value.items +
                    AgentMessageItem.UserMessage(content) +
                    AgentMessageItem.AssistantMessage(message, taskId = 0L),
                error = null
            )
            saveMessageToDatabase(content, isUser = true)
            saveMessageToDatabase(message, isUser = false)
            return
        }

        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items + AgentMessageItem.UserMessage(content),
            error = null
        )
        saveMessageToDatabase(content, isUser = true)
        confirmAction(pendingItem.taskId, pendingItem.action.actionId)
    }

    private fun String?.isWaitingForConfirmation(): Boolean =
        this == null || this == "WAITING_CONFIRMATION"
    
    /**
     * 处理任务响应
     */
    private fun handleTaskResponse(taskResponse: com.fitness.training.network.dto.AgentTaskResponse) {
        // 移除加载中指示器
        val itemsWithoutLoading = _uiState.value.items.filterNot { it is AgentMessageItem.LoadingMessage }
        
        val newItems = mutableListOf<AgentMessageItem>()
        newItems.addAll(itemsWithoutLoading)
        
        // 添加进度步骤（如果有）
        if (taskResponse.steps.isNotEmpty()) {
            val stepLabels = taskResponse.steps.map { it.label }
            newItems.add(
                AgentMessageItem.ProgressStep(
                    taskId = taskResponse.taskId,
                    steps = stepLabels
                )
            )
        }
        
        // 添加助手消息（如果有）
        if (!taskResponse.assistantMessage.isNullOrEmpty()) {
            newItems.add(
                AgentMessageItem.AssistantMessage(
                    content = taskResponse.assistantMessage,
                    taskId = taskResponse.taskId
                )
            )
            // 保存助手消息到数据库
            saveMessageToDatabase(taskResponse.assistantMessage, isUser = false)
        }
        
        // 每份草案是独立的卡片。服务端新契约返回完整列表，旧服务端回退单项。
        taskResponse.actionsForDisplay().forEach { pendingAction ->
            if (pendingAction.status != "LOCAL_WRITE_PENDING" && pendingAction.expiresAt > System.currentTimeMillis()) {
                newItems.add(
                    AgentMessageItem.PendingActionItem(
                        taskId = taskResponse.taskId,
                        action = pendingAction
                    )
                )
            } else {
                newItems.add(
                    AgentMessageItem.AssistantMessage(
                        content = "⚠️ 该操作已过期，请重新发起请求",
                        taskId = taskResponse.taskId
                    )
                )
            }
        }
        
        // 处理失败状态
        if (taskResponse.status == "FAILED" && taskResponse.failureReason != null) {
            newItems.add(
                AgentMessageItem.ErrorMessage(
                    message = "任务失败: ${taskResponse.failureReason}",
                    taskId = taskResponse.taskId
                )
            )
        }
        
        _uiState.value = _uiState.value.copy(
            items = newItems,
            activeTaskId = taskResponse.taskId
        )
    }
    
    /**
     * 处理错误
     */
    private fun handleError(message: String) {
        // 移除加载中指示器
        val itemsWithoutLoading = _uiState.value.items.filterNot { it is AgentMessageItem.LoadingMessage }
        
        // 修复2: 重置isSubmitting状态，避免按钮保持禁用
        _uiState.value = _uiState.value.copy(
            items = itemsWithoutLoading + AgentMessageItem.ErrorMessage(message),
            error = message,
            isSubmitting = false
        )
    }
    
    /**
     * 保存训练计划草案到本地（已废弃，使用confirmAction代替）
     */
    @Deprecated("使用confirmAction方法代替")
    fun savePlanToLocal(taskId: Long, @Suppress("UNUSED_PARAMETER") actionId: Long) {
        if (_uiState.value.isSubmitting) return
        
        _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
        
        viewModelScope.launch {
            try {
                val token = "Bearer ${UserSession.getToken(getApplication())}"
                
                // 获取任务详情（包含完整的payloadJson）
                val response = withContext(Dispatchers.IO) {
                    apiService.getAgentTask(token, taskId)
                }
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val taskResponse = response.body()?.data
                    val pendingAction = taskResponse?.pendingAction
                    
                    if (pendingAction != null && pendingAction.type == "CREATE_TRAINING_PLAN") {
                        // 解析训练计划草案JSON
                        val planJson = pendingAction.payload ?: ""
                        
                        if (planJson.isNotEmpty()) {
                            // TODO: 解析JSON并保存到Room数据库
                            // 这里需要你的TrainingPlan实体类
                            // val plan = parsePlanJson(planJson)
                            // database.trainingPlanDao().insert(plan)
                            
                            // 临时：直接显示成功消息
                            withContext(Dispatchers.Main) {
                                // F1-B修复：按actionId移除待确认项，而不是taskId（支持多草案）
                                val updatedItems = _uiState.value.items.filterNot { 
                                    it is AgentMessageItem.PendingActionItem && it.action.actionId == actionId 
                                }
                                
                                _uiState.value = _uiState.value.copy(
                                    items = updatedItems + AgentMessageItem.AssistantMessage(
                                        content = "✅ 训练计划已保存到本地！你可以在训练计划模块中查看。",
                                        taskId = taskId
                                    ),
                                    isSubmitting = false
                                )
                            }
                        } else {
                            handleError("训练计划数据为空")
                        }
                    } else {
                        handleError("未找到训练计划草案")
                    }
                } else {
                    handleError("获取训练计划失败: ${response.body()?.message ?: response.message()}")
                }
            } catch (e: Exception) {
                handleError("网络错误: ${e.message}")
            }
        }
    }
    
    /**
     * 确认待执行操作（两阶段提交流程）
     * 
     * 流程：
     * 1. 调用后端 confirmAction（第一阶段：获取执行权，状态变为 LOCAL_WRITE_PENDING）
     * 2. 本地保存训练计划到 Room 数据库
     * 3. 调用后端 completeLocalWrite（第二阶段：确认本地写入成功，状态变为 SUCCEEDED）
     * 
     * 错误恢复：
     * - 如果第一阶段失败：保持 WAITING_CONFIRMATION，用户可重试
     * - 如果本地保存失败：保持 LOCAL_WRITE_PENDING，应用启动时恢复
     * - 如果第二阶段网络失败：本地已保存，应用启动时重试完成
     */
    fun confirmAction(taskId: Long, actionId: Long) {
        if (_uiState.value.isSubmitting) return
        
        _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
        
        viewModelScope.launch {
            try {
                // 获取 pendingAction 中的 payload 和类型
                val currentItems = _uiState.value.items
                val pendingItem = currentItems.firstOrNull { 
                    it is AgentMessageItem.PendingActionItem
                        && it.taskId == taskId
                        && it.action.actionId == actionId
                } as? AgentMessageItem.PendingActionItem
                
                if (pendingItem == null) {
                    handleError("待确认操作不存在")
                    return@launch
                }
                
                val payload = pendingItem.action.payload
                val actionType = pendingItem.action.type
                val userId = UserSession.getCurrentUserId(getApplication())
                
                // 数据校验：训练计划必须有 payload
                if (actionType == "CREATE_TRAINING_PLAN" && payload.isNullOrBlank()) {
                    handleError("训练计划草案数据缺失，无法确认")
                    return@launch
                }
                
                if (actionType == "CREATE_TRAINING_PLAN" && !payload.isNullOrBlank()) {
                    // === 训练计划：两阶段提交流程 ===
                    
                    // 阶段1: 调用后端获取执行权
                    val token = "Bearer ${UserSession.getToken(getApplication())}"
                    val confirmRequest = com.fitness.training.network.dto.ConfirmActionRequest(actionId)
                    
                    val confirmResponse = withContext(Dispatchers.IO) {
                        apiService.confirmAgentAction(token, taskId, confirmRequest)
                    }
                    
                    if (!confirmResponse.isSuccessful || confirmResponse.body()?.code != 200) {
                        // 第一阶段失败，后端仍为 WAITING_CONFIRMATION
                        handleError("确认失败: ${confirmResponse.body()?.message ?: confirmResponse.message()}")
                        return@launch
                    }
                    
                    // 阶段2: 本地保存训练计划
                    var localPlanId: Long = 0
                    try {
                        val gson = com.google.gson.Gson()
                        val draft = gson.fromJson(payload, com.fitness.training.network.dto.TrainingPlanDraft::class.java)
                        
                        val normalizedDraft = normalizeTrainingPlanDraft(draft)
                        
                        val detailsText = buildTrainingPlanDetails(normalizedDraft.days)
                        
                        // 创建本地训练计划实体（带 agentActionId）
                        val trainingPlan = com.fitness.training.data.entity.TrainingPlan(
                            userId = userId,
                            title = normalizedDraft.title,
                            description = normalizedDraft.description,
                            goal = normalizedDraft.goal,
                            experience = normalizedDraft.experience,
                            targetMuscles = normalizedDraft.targetMuscles,
                            trainingDays = normalizedDraft.trainingDays,
                            trainingDuration = normalizedDraft.trainingDuration,
                            equipment = normalizedDraft.equipment,
                            details = detailsText,
                            isPinned = false,
                            isFromRecommendation = true,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            agentActionId = actionId  // 关联 actionId，用于幂等性
                        )
                        
                        // 保存到本地 Room 数据库
                        localPlanId = withContext(Dispatchers.IO) {
                            database.trainingPlanDao().insertPlanIdempotent(trainingPlan)
                        }
                        
                    } catch (e: Exception) {
                        // 本地保存失败时，服务端仍保持 LOCAL_WRITE_PENDING；记录完整上下文以支持显式重试。
                        recordRecoveryFailure(
                            userId, taskId, actionId, actionType, null,
                            e.message ?: "本地保存训练计划失败"
                        )
                        handleError("本地保存失败: ${e.message}\n请重试同步或重启应用后恢复")
                        return@launch
                    }
                    
                    // 阶段3: 调用后端完成写入
                    var completeSuccess = false
                    try {
                        val completeRequest = com.fitness.training.network.dto.CompleteLocalWriteRequest(
                            actionId = actionId,
                            localReference = com.fitness.training.network.dto.LocalWriteReference
                                .trainingPlan(localPlanId)
                        )
                        
                        val completeResponse = withContext(Dispatchers.IO) {
                            apiService.completeLocalWrite(token, taskId, actionId, completeRequest)
                        }
                        
                        if (completeResponse.isSuccessful && completeResponse.body()?.code == 200) {
                            completeSuccess = true
                            clearRecoveryFailure(userId, actionId)
                        } else {
                            recordRecoveryFailure(
                                userId, taskId, actionId, actionType,
                                com.fitness.training.network.dto.LocalWriteReference.trainingPlan(localPlanId),
                                completeResponse.body()?.message ?: "完成回执被服务器拒绝"
                            )
                        }
                        
                    } catch (e: Exception) {
                        recordRecoveryFailure(
                            userId, taskId, actionId, actionType,
                            com.fitness.training.network.dto.LocalWriteReference.trainingPlan(localPlanId),
                            e.message ?: "完成回执网络错误"
                        )
                    }
                    
                    // 根据完成状态显示不同的提示
                    val successMessage = if (completeSuccess) {
                        "✅ 训练计划已保存！你可以在个人计划中查看。"
                    } else {
                        "✅ 训练计划已保存到本地！网络恢复后会自动同步。"
                    }
                    
                    withContext(Dispatchers.Main) {
                        // F1-B修复：按actionId移除待确认项，而不是taskId（支持多草案）
                        val updatedItems = _uiState.value.items.filterNot { 
                            it is AgentMessageItem.PendingActionItem && it.action.actionId == actionId 
                        }
                        
                        // 添加成功消息
                        _uiState.value = _uiState.value.copy(
                            items = updatedItems + AgentMessageItem.AssistantMessage(
                                content = successMessage,
                                taskId = taskId
                            ),
                            isSubmitting = false
                        )
                        
                        // 保存消息到数据库
                        saveMessageToDatabase(successMessage, isUser = false)
                    }
                    
                } else if (actionType == "CREATE_DIET_RECORD" && !payload.isNullOrBlank()) {
                    // === 饮食记录：两阶段提交流程 ===
                    
                    // 阶段1: 调用后端获取执行权
                    val token = "Bearer ${UserSession.getToken(getApplication())}"
                    val confirmRequest = com.fitness.training.network.dto.ConfirmActionRequest(actionId)
                    
                    val confirmResponse = withContext(Dispatchers.IO) {
                        apiService.confirmAgentAction(token, taskId, confirmRequest)
                    }
                    
                    if (!confirmResponse.isSuccessful || confirmResponse.body()?.code != 200) {
                        // 第一阶段失败，后端仍为 WAITING_CONFIRMATION
                        handleError("确认失败: ${confirmResponse.body()?.message ?: confirmResponse.message()}")
                        return@launch
                    }
                    
                    // 阶段2: 本地保存饮食记录
                    var localRefId: String = "0"
                    try {
                        val gson = com.google.gson.Gson()
                        val draft = gson.fromJson(payload, com.fitness.training.network.dto.DietRecordDraft::class.java)
                        
                        // 严格校验
                        if (draft.date.isBlank()) {
                            throw IllegalArgumentException("饮食记录数据不完整：日期为空")
                        }
                        if (draft.records.isEmpty()) {
                            throw IllegalArgumentException("饮食记录数据不完整：没有任何食物")
                        }
                        
                        // 解析日期字符串为时间戳（yyyy-MM-dd -> timestamp）
                        val dateTimestamp = try {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            sdf.parse(draft.date)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            throw IllegalArgumentException("日期格式错误: ${draft.date}")
                        }
                        
                        // 创建本地饮食记录实体列表（带 recordKey）
                        val dietRecords = draft.records.mapIndexed { index, item ->
                            // 生成稳定的 recordKey：格式 "mealType_foodName_index"
                            // 确保同一草案的每条记录有唯一的 key
                            val recordKey = "${item.mealType}_${item.foodName}_$index"
                            
                            com.fitness.training.data.entity.DietRecord(
                                userId = userId,
                                agentActionId = actionId,  // 关联 actionId，用于幂等性
                                recordKey = recordKey,     // 记录键，用于精确幂等性
                                date = dateTimestamp,
                                mealType = item.mealType,
                                foodName = item.foodName,
                                calories = item.calories,
                                protein = item.protein,
                                carbs = item.carbs,
                                fat = item.fat,
                                amount = item.amount
                            )
                        }
                        
                        // 批量保存到本地 Room 数据库（幂等）
                        localRefId = withContext(Dispatchers.IO) {
                            database.dietRecordDao().insertBatchIdempotent(userId, actionId, dietRecords)
                        }
                        
                    } catch (e: Exception) {
                        // 本地保存失败时，服务端仍保持 LOCAL_WRITE_PENDING；记录完整上下文以支持显式重试。
                        recordRecoveryFailure(
                            userId, taskId, actionId, actionType, null,
                            e.message ?: "本地保存饮食记录失败"
                        )
                        handleError("本地保存失败: ${e.message}\n请重试同步或重启应用后恢复")
                        return@launch
                    }
                    
                    // 阶段3: 调用后端完成写入
                    var completeSuccess = false
                    try {
                        val completeRequest = com.fitness.training.network.dto.CompleteLocalWriteRequest(
                            actionId = actionId,
                            localReference = localRefId
                        )
                        
                        val completeResponse = withContext(Dispatchers.IO) {
                            apiService.completeLocalWrite(token, taskId, actionId, completeRequest)
                        }
                        
                        if (completeResponse.isSuccessful && completeResponse.body()?.code == 200) {
                            completeSuccess = true
                            clearRecoveryFailure(userId, actionId)
                        } else {
                            recordRecoveryFailure(
                                userId, taskId, actionId, actionType, localRefId,
                                completeResponse.body()?.message ?: "完成回执被服务器拒绝"
                            )
                        }
                        
                    } catch (e: Exception) {
                        recordRecoveryFailure(
                            userId, taskId, actionId, actionType, localRefId,
                            e.message ?: "完成回执网络错误"
                        )
                    }
                    
                    // 根据完成状态显示不同的提示
                    val successMessage = if (completeSuccess) {
                        "✅ 饮食记录已保存！你可以在饮食模块中查看。"
                    } else {
                        "✅ 饮食记录已保存到本地！网络恢复后会自动同步。"
                    }
                    
                    withContext(Dispatchers.Main) {
                        // F1-B修复：按actionId移除待确认项，而不是taskId（支持多草案）
                        val updatedItems = _uiState.value.items.filterNot { 
                            it is AgentMessageItem.PendingActionItem && it.action.actionId == actionId 
                        }
                        
                        // 添加成功消息
                        _uiState.value = _uiState.value.copy(
                            items = updatedItems + AgentMessageItem.AssistantMessage(
                                content = successMessage,
                                taskId = taskId
                            ),
                            isSubmitting = false
                        )
                        
                        // 保存消息到数据库
                        saveMessageToDatabase(successMessage, isUser = false)
                    }
                    
                } else {
                    // === 其他类型的确认操作 ===
                    // 不支持的类型，直接报错
                    handleError("不支持的操作类型: $actionType")
                }
            } catch (e: Exception) {
                handleError("网络错误: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isSubmitting = false)
            }
        }
    }
    
    /**
     * 取消单个草案，不影响同一任务中的其他待确认动作。
     */
    fun cancelAction(taskId: Long, actionId: Long) {
        if (_uiState.value.isSubmitting) return
        _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)

        viewModelScope.launch {
            try {
                val token = "Bearer ${UserSession.getToken(getApplication())}"
                val response = withContext(Dispatchers.IO) {
                    apiService.cancelAgentAction(token, taskId, actionId)
                }
                if (response.isSuccessful && response.body()?.code == 200) {
                    val updatedItems = _uiState.value.items.filterNot {
                        it is AgentMessageItem.PendingActionItem && it.action.actionId == actionId
                    }
                    _uiState.value = _uiState.value.copy(
                        items = updatedItems + AgentMessageItem.AssistantMessage(
                            content = "已取消操作",
                            taskId = taskId
                        )
                    )
                } else {
                    handleError("取消失败: ${response.body()?.message ?: response.message()}")
                }
            } catch (e: Exception) {
                handleError("网络错误: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isSubmitting = false)
            }
        }
    }

    /**
     * 取消任务
     */
    fun cancelTask(taskId: Long) {
        if (_uiState.value.isSubmitting) return
        
        _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
        
        viewModelScope.launch {
            try {
                val token = "Bearer ${UserSession.getToken(getApplication())}"
                
                val response = withContext(Dispatchers.IO) {
                    apiService.cancelAgentTask(token, taskId)
                }
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    // F1-B: 取消任务时，移除该任务的所有待确认项
                    val updatedItems = _uiState.value.items.filterNot { 
                        it is AgentMessageItem.PendingActionItem && it.taskId == taskId 
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        items = updatedItems + AgentMessageItem.AssistantMessage(
                            content = "已取消操作",
                            taskId = taskId
                        )
                    )
                } else {
                    handleError("取消失败: ${response.body()?.message ?: response.message()}")
                }
            } catch (e: Exception) {
                handleError("网络错误: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isSubmitting = false)
            }
        }
    }
    
    /**
     * 清空聊天记录
     */
    fun clearMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = UserSession.getCurrentUserId(getApplication())
            database.aiChatMessageDao().deleteAllMessages(userId)
            
            withContext(Dispatchers.Main) {
                val welcomeItem = AgentMessageItem.AssistantMessage(
                    content = "👋 你好！我是你的智能健身助手。\n\n你可以问我：\n• 训练动作的正确姿势\n• 制定训练计划\n• 饮食和营养建议\n• 分析你的训练数据\n\n有什么可以帮你的？",
                    taskId = 0L
                )
                _uiState.value = AgentUiState(
                    items = listOf(welcomeItem),
                    sessionId = UUID.randomUUID().toString()
                )
            }
        }
    }
    
    /**
     * 保存消息到数据库
     */
    private fun saveMessageToDatabase(content: String, isUser: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val msgUserId = UserSession.getCurrentUserId(getApplication())
            database.aiChatMessageDao().insert(
                AiChatMessage(
                    userId = msgUserId,
                    content = content,
                    isUser = isUser
                )
            )
        }
    }
    
    /**
     * 恢复未完成的本地写入操作
     * 
     * 在应用启动时检查是否有 LOCAL_WRITE_PENDING 状态的操作：
     * 1. 查询后端所有 LOCAL_WRITE_PENDING 操作
     * 2. 检查本地是否已保存对应的训练计划（通过 agentActionId）
     * 3. 如果已保存，调用 completeLocalWrite 完成第二阶段
     * 4. 如果未保存，重新执行保存流程
     */
    private fun recoverPendingLocalWrites() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = "Bearer ${UserSession.getToken(getApplication())}"
                val userId = UserSession.getCurrentUserId(getApplication())
                
                android.util.Log.i("AgentViewModel", "========== 开始恢复待完成的本地写入操作 ==========")
                
                // 查询后端所有 LOCAL_WRITE_PENDING 操作
                val response = apiService.getPendingLocalWrites(token)
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val pendingWrites = response.body()?.data ?: emptyList()
                    clearFailuresOutsidePendingWrites(
                        userId,
                        pendingWrites.map { it.actionId }.toSet()
                    )
                    
                    if (pendingWrites.isEmpty()) {
                        android.util.Log.i("AgentViewModel", "没有待完成的操作")
                        withContext(Dispatchers.Main) { publishRecoveryItems(userId) }
                        return@launch
                    }
                    
                    android.util.Log.i("AgentViewModel", "发现 ${pendingWrites.size} 个待完成操作")
                    
                    for (write in pendingWrites) {
                        android.util.Log.i("AgentViewModel", """
                            ========== 恢复操作 ==========
                            类型: ${write.type}
                            TaskID: ${write.taskId}
                            ActionID: ${write.actionId}
                            ================================
                        """.trimIndent())
                        
                        try {
                            recoverLocalWrite(userId, write)
                        } catch (e: Exception) {
                            android.util.Log.e("AgentViewModel", """
                                ========== 恢复失败 ==========
                                类型: ${write.type}
                                ActionID: ${write.actionId}
                                错误: ${e.message}
                                堆栈: ${android.util.Log.getStackTraceString(e)}
                                ================================
                            """.trimIndent(), e)
                            
                            recordRecoveryFailure(
                                userId, write.taskId, write.actionId, write.type,
                                null, e.message ?: "未知错误"
                            )
                        }
                    }
                    
                    withContext(Dispatchers.Main) { publishRecoveryItems(userId) }
                    android.util.Log.i("AgentViewModel", "========== 恢复操作完成 ==========")
                } else {
                    android.util.Log.e("AgentViewModel", 
                        "查询待完成操作失败: code=${response.body()?.code}, message=${response.body()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("AgentViewModel", "恢复本地写入网络错误: ${e.message}", e)
                val userId = UserSession.getCurrentUserId(getApplication())
                withContext(Dispatchers.Main) { publishRecoveryItems(userId) }
            }
        }
    }
    
    private fun recoveryPreferences() = getApplication<android.app.Application>()
        .getSharedPreferences("agent_recovery", android.content.Context.MODE_PRIVATE)

    private fun recoveryFailureKey(userId: Long, actionId: Long): String =
        "failure_${userId}_$actionId"

    private fun storedRecoveryFailures(userId: Long): List<LocalWriteRecoveryFailure> =
        recoveryPreferences().all.values.mapNotNull { raw ->
            (raw as? String)?.let { json ->
                runCatching { com.google.gson.Gson().fromJson(json, LocalWriteRecoveryFailure::class.java) }.getOrNull()
            }
        }.filter { it.userId == userId }

    private fun clearFailuresOutsidePendingWrites(userId: Long, pendingActionIds: Set<Long>) {
        val retainedActionIds = LocalWriteRecoveryState
            .retainPending(storedRecoveryFailures(userId), pendingActionIds)
            .mapTo(mutableSetOf()) { it.actionId }
        storedRecoveryFailures(userId)
            .filterNot { it.actionId in retainedActionIds }
            .forEach { clearRecoveryFailure(userId, it.actionId) }
    }

    private fun publishRecoveryItems(userId: Long) {
        val recoveryItems = LocalWriteRecoveryState.toUiItems(storedRecoveryFailures(userId))
        val retainedItems = _uiState.value.items.filterNot {
            it is AgentMessageItem.LocalWriteRecoveryItem
        }
        _uiState.value = _uiState.value.copy(items = retainedItems + recoveryItems)
    }

    /**
     * 重新读取服务端当前待回执动作，再按原有幂等逻辑执行恢复。
     */
    fun retryLocalWrite(taskId: Long, actionId: Long) {
        if (_uiState.value.isSubmitting) return
        _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)

        viewModelScope.launch {
            val userId = UserSession.getCurrentUserId(getApplication())
            try {
                val token = "Bearer ${UserSession.getToken(getApplication())}"
                val pendingWrite = withContext(Dispatchers.IO) {
                    val response = apiService.getPendingLocalWrites(token)
                    if (!response.isSuccessful || response.body()?.code != 200) {
                        throw IllegalStateException(response.body()?.message ?: "无法获取待同步操作")
                    }
                    response.body()?.data?.firstOrNull {
                        it.taskId == taskId && it.actionId == actionId
                    }
                }
                if (pendingWrite == null) {
                    clearRecoveryFailure(userId, actionId)
                } else {
                    withContext(Dispatchers.IO) {
                        recoverLocalWrite(userId, pendingWrite)
                    }
                }
            } catch (e: Exception) {
                val failure = storedRecoveryFailures(userId).firstOrNull { it.actionId == actionId }
                if (failure != null) {
                    recordRecoveryFailure(
                        userId, taskId, actionId, failure.type, failure.localReference,
                        e.message ?: "重试同步失败"
                    )
                }
                _uiState.value = _uiState.value.copy(error = "重试同步失败: ${e.message}")
            } finally {
                publishRecoveryItems(userId)
                _uiState.value = _uiState.value.copy(isSubmitting = false)
            }
        }
    }

    private suspend fun recoverLocalWrite(
        userId: Long,
        write: com.fitness.training.network.dto.PendingLocalWrite
    ) {
        when (write.type) {
            "CREATE_TRAINING_PLAN" -> recoverTrainingPlan(userId, write)
            "CREATE_DIET_RECORD" -> recoverDietRecord(userId, write)
            else -> throw IllegalArgumentException("不支持恢复的操作类型: ${write.type}")
        }
    }

    private fun recordRecoveryFailure(
        userId: Long,
        taskId: Long,
        actionId: Long,
        type: String,
        localReference: String?,
        error: String
    ) {
        try {
            val prefs = recoveryPreferences()
            val failureKey = recoveryFailureKey(userId, actionId)
            val retryCount = prefs.getInt("${failureKey}_retry", 0)
            val failure = LocalWriteRecoveryState.nextFailure(
                userId = userId,
                taskId = taskId,
                actionId = actionId,
                type = type,
                localReference = localReference,
                error = error,
                timestamp = System.currentTimeMillis(),
                previousRetryCount = retryCount
            )

            prefs.edit()
                .putString(failureKey, com.google.gson.Gson().toJson(failure))
                .putInt("${failureKey}_retry", failure.retryCount)
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("AgentViewModel", "记录本地回执失败状态出错", e)
        }
    }

    private fun clearRecoveryFailure(userId: Long, actionId: Long) {
        val prefs = recoveryPreferences()
        val failureKey = recoveryFailureKey(userId, actionId)
        prefs.edit()
            .remove(failureKey)
            .remove("${failureKey}_retry")
            .apply()
    }
    
    /**
     * 恢复训练计划的本地写入
     */
    private suspend fun recoverTrainingPlan(userId: Long, write: com.fitness.training.network.dto.PendingLocalWrite) {
        try {
            android.util.Log.i("AgentViewModel", "检查训练计划是否已保存: actionId=${write.actionId}")
            
            // 检查本地是否已保存该训练计划
            val existingPlan = database.trainingPlanDao()
                .getPlanByActionId(userId, write.actionId)
            
            if (existingPlan != null) {
                // 本地已保存，直接完成回执
                android.util.Log.i("AgentViewModel", """
                    训练计划已存在，完成回执
                    - ActionID: ${write.actionId}
                    - PlanID: ${existingPlan.id}
                    - Title: ${existingPlan.title}
                """.trimIndent())
                
                completeLocalWriteSilently(
                    userId,
                    write.taskId,
                    write.actionId,
                    write.type,
                    com.fitness.training.network.dto.LocalWriteReference.trainingPlan(existingPlan.id)
                )
            } else {
                // 本地未保存，重新保存
                android.util.Log.w("AgentViewModel", "训练计划不存在，重新保存: actionId=${write.actionId}")
                
                retryTrainingPlanSave(userId, write)
            }
        } catch (e: Exception) {
            android.util.Log.e("AgentViewModel", """
                恢复训练计划失败
                - ActionID: ${write.actionId}
                - Error: ${e.message}
                - Stack: ${android.util.Log.getStackTraceString(e)}
            """.trimIndent(), e)
            throw e  // 向上抛出，记录到失败日志
        }
    }
    
    private suspend fun completeLocalWriteSilently(
        userId: Long,
        taskId: Long,
        actionId: Long,
        type: String,
        localReference: String
    ): Boolean {
        return try {
            val token = "Bearer ${UserSession.getToken(getApplication())}"
            val request = com.fitness.training.network.dto.CompleteLocalWriteRequest(
                actionId = actionId,
                localReference = localReference
            )
            val response = apiService.completeLocalWrite(token, taskId, actionId, request)

            if (response.isSuccessful && response.body()?.code == 200) {
                clearRecoveryFailure(userId, actionId)
                true
            } else {
                recordRecoveryFailure(
                    userId, taskId, actionId, type, localReference,
                    response.body()?.message ?: "完成回执被服务器拒绝"
                )
                false
            }
        } catch (e: Exception) {
            recordRecoveryFailure(
                userId, taskId, actionId, type, localReference,
                e.message ?: "完成回执网络错误"
            )
            false
        }
    }
    
    private data class NormalizedTrainingPlanDraft(
        val title: String,
        val description: String,
        val goal: String,
        val experience: String,
        val targetMuscles: String,
        val trainingDays: Int,
        val trainingDuration: String,
        val equipment: String,
        val days: List<NormalizedDayPlan>
    )

    private data class NormalizedDayPlan(
        val name: String,
        val focus: String,
        val exercises: List<NormalizedExercise>
    )

    private data class NormalizedExercise(
        val name: String,
        val sets: Int,
        val reps: String,
        val restTime: String,
        val notes: String?
    )

    private fun normalizeTrainingPlanDraft(
        draft: com.fitness.training.network.dto.TrainingPlanDraft
    ): NormalizedTrainingPlanDraft {
        fun requireNonBlank(value: String?, field: String): String =
            value?.trim()?.takeIf(String::isNotBlank)
                ?: throw IllegalArgumentException("训练计划数据不完整：$field 为空或缺失")

        val days = draft.days ?: throw IllegalArgumentException("训练计划数据不完整：训练安排缺失")
        if (days.isEmpty()) {
            throw IllegalArgumentException("训练计划数据不完整：没有训练安排")
        }
        if (days.size != draft.trainingDays) {
            throw IllegalArgumentException("训练计划数据不一致：训练天数与计划不匹配")
        }

        return NormalizedTrainingPlanDraft(
            title = requireNonBlank(draft.title, "标题"),
            description = draft.description?.trim().orEmpty(),
            goal = requireNonBlank(draft.goal, "目标"),
            experience = requireNonBlank(draft.experience, "经验等级"),
            targetMuscles = draft.targetMuscles?.trim().orEmpty(),
            trainingDays = draft.trainingDays,
            trainingDuration = draft.trainingDuration?.trim().takeUnless { it.isNullOrEmpty() } ?: "标准",
            equipment = draft.equipment?.trim().takeUnless { it.isNullOrEmpty() } ?: "健身房",
            days = days.mapIndexed { dayIndex, day ->
                val exercises = day.exercises ?: throw IllegalArgumentException(
                    "训练计划数据不完整：第${dayIndex + 1}天动作列表缺失"
                )
                if (exercises.isEmpty()) {
                    throw IllegalArgumentException("训练计划数据不完整：第${dayIndex + 1}天没有训练动作")
                }
                NormalizedDayPlan(
                    name = requireNonBlank(day.name, "第${dayIndex + 1}天名称"),
                    focus = requireNonBlank(day.focus, "第${dayIndex + 1}天训练重点"),
                    exercises = exercises.mapIndexed { exerciseIndex, exercise ->
                        NormalizedExercise(
                            name = requireNonBlank(exercise.name, "第${dayIndex + 1}天第${exerciseIndex + 1}个动作名称"),
                            sets = exercise.sets.takeIf { it > 0 }
                                ?: throw IllegalArgumentException("训练计划数据不完整：动作组数必须大于0"),
                            reps = requireNonBlank(exercise.reps, "动作次数"),
                            restTime = requireNonBlank(exercise.restTime, "动作休息时间"),
                            notes = exercise.notes?.trim()?.takeIf(String::isNotBlank)
                        )
                    }
                )
            }
        )
    }

    private fun buildTrainingPlanDetails(days: List<NormalizedDayPlan>): String = buildString {
        for (day in days) {
            append("═══ ${day.name}：${day.focus} ═══\n\n")
            for ((index, exercise) in day.exercises.withIndex()) {
                append("${index + 1}. ${exercise.name} - ${exercise.sets}组 x ${exercise.reps}\n")
                exercise.notes?.let { append("   提示：$it\n") }
                append("   组间休息：${exercise.restTime}\n")
            }
            append("\n────────────────────\n\n")
        }
    }

    /**
     * 重试训练计划保存
     */
    private suspend fun retryTrainingPlanSave(userId: Long, write: com.fitness.training.network.dto.PendingLocalWrite) {
        try {
            android.util.Log.i("AgentViewModel", "开始重新保存训练计划: actionId=${write.actionId}")
            
            val gson = com.google.gson.Gson()
            val draft = gson.fromJson(
                write.payloadJson, 
                com.fitness.training.network.dto.TrainingPlanDraft::class.java
            )
            
            val normalizedDraft = normalizeTrainingPlanDraft(draft)
            val detailsText = buildTrainingPlanDetails(normalizedDraft.days)
            
            // 创建训练计划实体
            val trainingPlan = com.fitness.training.data.entity.TrainingPlan(
                userId = userId,
                title = normalizedDraft.title,
                description = normalizedDraft.description,
                goal = normalizedDraft.goal,
                experience = normalizedDraft.experience,
                targetMuscles = normalizedDraft.targetMuscles,
                trainingDays = normalizedDraft.trainingDays,
                trainingDuration = normalizedDraft.trainingDuration,
                equipment = normalizedDraft.equipment,
                details = detailsText,
                isPinned = false,
                isFromRecommendation = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                agentActionId = write.actionId
            )
            
            // 保存到本地
            val localPlanId = database.trainingPlanDao().insertPlanIdempotent(trainingPlan)
            
            android.util.Log.i("AgentViewModel", """
                ✅ 重新保存训练计划成功
                - ActionID: ${write.actionId}
                - PlanID: $localPlanId
                - Title: ${normalizedDraft.title}
            """.trimIndent())
            
            // 完成回执
            completeLocalWriteSilently(
                userId,
                write.taskId,
                write.actionId,
                write.type,
                com.fitness.training.network.dto.LocalWriteReference.trainingPlan(localPlanId)
            )
            
        } catch (e: Exception) {
            android.util.Log.e("AgentViewModel", """
                ❌ 重新保存训练计划失败
                - ActionID: ${write.actionId}
                - Error: ${e.message}
                - Stack: ${android.util.Log.getStackTraceString(e)}
            """.trimIndent(), e)
            throw e
        }
    }
    
    /**
     * 恢复饮食记录的本地写入
     */
    private suspend fun recoverDietRecord(userId: Long, write: com.fitness.training.network.dto.PendingLocalWrite) {
        try {
            android.util.Log.i("AgentViewModel", "检查饮食记录是否已保存: actionId=${write.actionId}")
            
            // 检查本地是否已保存该饮食记录（一个actionId可能对应多条记录）
            val existingRecords = database.dietRecordDao()
                .getAllByActionId(userId, write.actionId)
            
            if (existingRecords.isNotEmpty()) {
                android.util.Log.i("AgentViewModel", """
                    发现已有饮食记录，按草案补齐并重建稳定回执引用
                    - ActionID: ${write.actionId}
                    - ExistingCount: ${existingRecords.size}
                    - ExistingIDs: ${existingRecords.joinToString(",") { it.id.toString() }}
                """.trimIndent())
            } else {
                android.util.Log.w("AgentViewModel", "饮食记录不存在，按草案重新保存: actionId=${write.actionId}")
            }

            // 不以“存在任意记录”作为完整写入依据。该路径会按草案顺序补齐缺项，
            // 并复用 insertBatchIdempotent 的草案顺序回传同一份 localReference。
            retryDietRecordSave(userId, write)
        } catch (e: Exception) {
            android.util.Log.e("AgentViewModel", """
                恢复饮食记录失败
                - ActionID: ${write.actionId}
                - Error: ${e.message}
                - Stack: ${android.util.Log.getStackTraceString(e)}
            """.trimIndent(), e)
            throw e
        }
    }
    
    /**
     * 重试饮食记录保存
     */
    private suspend fun retryDietRecordSave(userId: Long, write: com.fitness.training.network.dto.PendingLocalWrite) {
        try {
            android.util.Log.i("AgentViewModel", "开始重新保存饮食记录: actionId=${write.actionId}")
            
            val gson = com.google.gson.Gson()
            val draft = gson.fromJson(
                write.payloadJson, 
                com.fitness.training.network.dto.DietRecordDraft::class.java
            )
            
            // 校验草案数据
            if (draft.date.isBlank()) {
                android.util.Log.e("AgentViewModel", """
                    ❌ 饮食记录日期为空，无法恢复
                    - ActionID: ${write.actionId}
                """.trimIndent())
                throw IllegalArgumentException("饮食记录日期为空")
            }
            
            if (draft.records.isEmpty()) {
                android.util.Log.e("AgentViewModel", """
                    ❌ 饮食记录列表为空，无法恢复
                    - ActionID: ${write.actionId}
                    - Date: ${draft.date}
                """.trimIndent())
                throw IllegalArgumentException("饮食记录列表为空")
            }
            
            // 解析日期
            val dateTimestamp = try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                sdf.parse(draft.date)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                android.util.Log.e("AgentViewModel", """
                    ❌ 日期格式错误
                    - ActionID: ${write.actionId}
                    - Date: ${draft.date}
                    - Error: ${e.message}
                """.trimIndent(), e)
                throw IllegalArgumentException("日期格式错误: ${draft.date}")
            }
            
            android.util.Log.i("AgentViewModel", """
                准备保存 ${draft.records.size} 条饮食记录
                - Date: ${draft.date}
                - Records: ${draft.records.joinToString { "${it.mealType}-${it.foodName}" }}
            """.trimIndent())
            
            // 创建饮食记录实体列表（带 recordKey）
            val dietRecords = draft.records.mapIndexed { index, item ->
                // 校验每条记录
                if (item.mealType.isBlank()) {
                    throw IllegalArgumentException("第${index + 1}条记录：餐次类型为空")
                }
                if (item.foodName.isBlank()) {
                    throw IllegalArgumentException("第${index + 1}条记录：食物名称为空")
                }
                
                // 生成稳定的 recordKey：确保与确认流程一致
                val recordKey = "${item.mealType}_${item.foodName}_$index"
                
                com.fitness.training.data.entity.DietRecord(
                    userId = userId,
                    agentActionId = write.actionId,
                    recordKey = recordKey,
                    date = dateTimestamp,
                    mealType = item.mealType,
                    foodName = item.foodName,
                    calories = item.calories,
                    protein = item.protein,
                    carbs = item.carbs,
                    fat = item.fat,
                    amount = item.amount
                )
            }
            
            // 批量保存（幂等）
            val localRefId = database.dietRecordDao().insertBatchIdempotent(userId, write.actionId, dietRecords)
            
            android.util.Log.i("AgentViewModel", """
                ✅ 饮食记录保存成功
                - ActionID: ${write.actionId}
                - RefID: $localRefId
                - Count: ${dietRecords.size}
                - Date: ${draft.date}
            """.trimIndent())
            
            // 完成回执
            completeLocalWriteSilently(
                userId,
                write.taskId,
                write.actionId,
                write.type,
                localRefId
            )
            
        } catch (e: Exception) {
            android.util.Log.e("AgentViewModel", """
                ❌ 重新保存饮食记录失败
                - ActionID: ${write.actionId}
                - Error: ${e.message}
                - Stack: ${android.util.Log.getStackTraceString(e)}
            """.trimIndent(), e)
            throw e
        }
    }
    
    /**
     * 更新饮食草案（编辑后保存）
     * 
     * @param taskId 任务ID
     * @param actionId 操作ID
     * @param modifiedItems 修改后的食物列表
     */
    fun updateDraftPayload(taskId: Long, actionId: Long, date: String, modifiedItems: List<EditableFoodItem>) {
        if (_uiState.value.isSubmitting) return
        
        _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
        
        viewModelScope.launch {
            try {
                // 构建新的 payload JSON - 注意字段名要用 snake_case 匹配后端
                val gson = com.google.gson.Gson()
                val draftMap = mapOf(
                    "date" to date,
                    "records" to modifiedItems.map { item ->
                        mapOf(
                            "meal_type" to item.mealType,  // snake_case
                            "food_name" to item.foodName,  // snake_case
                            "amount" to item.amount,
                            "calories" to item.calories,
                            "protein" to item.protein,
                            "carbs" to item.carbs,
                            "fat" to item.fat,
                            "is_estimated" to item.isEstimated
                        )
                    }
                )
                val newPayloadJson = gson.toJson(draftMap)
                
                // 调用后端 API 更新草案
                val token = "Bearer ${UserSession.getToken(getApplication())}"
                val request = com.fitness.training.network.dto.UpdatePayloadRequest(newPayloadJson)
                
                val response = withContext(Dispatchers.IO) {
                    apiService.updateActionPayload(token, taskId, actionId, request)
                }
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val taskResponse = response.body()?.data
                    if (taskResponse != null) {
                        // 刷新 UI，显示更新后的草案
                        withContext(Dispatchers.Main) {
                            handleTaskResponse(taskResponse)
                        }
                    } else {
                        handleError("更新草案失败：服务器返回数据为空")
                    }
                } else {
                    handleError("更新草案失败: ${response.body()?.message ?: response.message()}")
                }
            } catch (e: Exception) {
                handleError("更新草案失败: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isSubmitting = false)
            }
        }
    }
}
