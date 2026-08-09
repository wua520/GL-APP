package com.fitness.training.ui.profile

import com.fitness.training.network.dto.PendingAction

/**
 * Agent UI 不可变状态
 */
data class AgentUiState(
    val sessionId: String = "",
    val items: List<AgentMessageItem> = emptyList(),
    val isSubmitting: Boolean = false,
    val activeTaskId: Long? = null,
    val error: String? = null
)

/**
 * Agent 消息项（聊天界面中的一行）
 */
sealed class AgentMessageItem {
    /**
     * 用户消息
     */
    data class UserMessage(
        val content: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : AgentMessageItem()
    
    /**
     * 助手消息
     */
    data class AssistantMessage(
        val content: String,
        val taskId: Long,
        val timestamp: Long = System.currentTimeMillis()
    ) : AgentMessageItem()
    
    /**
     * 进度步骤（例如：正在分析任务、已查询训练摘要）
     */
    data class ProgressStep(
        val taskId: Long,
        val steps: List<String>,  // 步骤描述列表
        val timestamp: Long = System.currentTimeMillis()
    ) : AgentMessageItem()
    
    /**
     * 待确认操作（例如：训练计划草案、饮食记录草案）
     */
    data class PendingActionItem(
        val taskId: Long,
        val action: PendingAction,
        val timestamp: Long = System.currentTimeMillis()
    ) : AgentMessageItem()
    
    /**
     * 可恢复的本地写入失败。该项由持久化状态重建，不依赖聊天记录是否仍在内存。
     */
    data class LocalWriteRecoveryItem(
        val taskId: Long,
        val actionId: Long,
        val type: String,
        val error: String,
        val retryCount: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : AgentMessageItem()

    /**
     * 错误消息
     */
    data class ErrorMessage(
        val message: String,
        val taskId: Long? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : AgentMessageItem()
    
    /**
     * 加载中（显示动画）
     */
    data class LoadingMessage(
        val timestamp: Long = System.currentTimeMillis()
    ) : AgentMessageItem()
}
