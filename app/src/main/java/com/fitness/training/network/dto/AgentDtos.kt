package com.fitness.training.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Agent 任务创建请求
 */
data class CreateAgentTaskRequest(
    val content: String,
    val sessionId: String? = null
)

/**
 * Agent 任务响应
 */
data class AgentTaskResponse(
    val taskId: Long,
    val status: String,
    val assistantMessage: String?,
    val steps: List<TaskStep>,
    val pendingAction: PendingAction?,
    val pendingActions: List<PendingAction>?,
    val failureReason: String?
) {
    fun actionsForDisplay(): List<PendingAction> = pendingActions ?: pendingAction?.let(::listOf) ?: emptyList()
}

/**
 * 任务执行步骤
 */
data class TaskStep(
    val type: String,        // ANALYZING, TOOL_CALL, RESPONDING, etc.
    val label: String,       // 用户可见的步骤描述
    val toolName: String?    // 如果是工具调用，记录工具名称
)

/**
 * 待确认操作
 */
data class PendingAction(
    val actionId: Long,
    val type: String,        // CREATE_TRAINING_PLAN, CREATE_DIET_RECORD
    val status: String?,
    val expiresAt: Long,
    val preview: Any?,       // 预览信息（可以是String或Object）
    val payload: String?     // 完整的草案JSON
)

/**
 * 操作预览（多态，根据type不同解析不同字段）
 */
data class ActionPreview(
    val title: String?,
    val description: String?,
    val details: Map<String, Any>?
)

/**
 * 确认操作请求
 */
data class ConfirmActionRequest(
    val actionId: Long
)

/**
 * 完成本地写入请求
 */
data class CompleteLocalWriteRequest(
    val actionId: Long,
    val localReference: String
)

/**
 * 待完成本地写入操作
 */
data class PendingLocalWrite(
    val taskId: Long,
    val actionId: Long,
    val type: String,
    val payloadJson: String,
    val createdAt: Long
)

/**
 * 取消任务请求（空body）
 */
class CancelTaskRequest

/**
 * 训练计划草案 - 用于解析payload
 */
data class TrainingPlanDraft(
    val title: String?,
    val description: String?,
    val goal: String?,
    val experience: String?,
    val targetMuscles: String?,
    val trainingDays: Int,
    val trainingDuration: String?,
    val equipment: String?,
    val days: List<DayPlan>?
)

data class DayPlan(
    val name: String?,
    val focus: String?,
    val exercises: List<Exercise>?
)

data class Exercise(
    val name: String?,
    val sets: Int,
    val reps: String?,
    val restTime: String?,
    val notes: String?
)

/**
 * 饮食记录草案 - 用于解析payload
 */
data class DietRecordDraft(
    val date: String,  // yyyy-MM-dd
    val records: List<DietRecordItem>
)

data class DietRecordItem(
    @SerializedName("meal_type")
    val mealType: String,  // 早餐、午餐、晚餐、加餐
    
    @SerializedName("food_name")
    val foodName: String,
    
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val amount: String,

    @SerializedName("is_estimated")
    val isEstimated: Boolean = false
)

/**
 * 更新操作草案请求
 */
data class UpdatePayloadRequest(
    val payloadJson: String
)
