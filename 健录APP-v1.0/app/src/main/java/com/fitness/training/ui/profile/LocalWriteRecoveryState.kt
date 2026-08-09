package com.fitness.training.ui.profile

/**
 * 某个服务端待回执动作在本地恢复失败后的持久化快照。
 */
data class LocalWriteRecoveryFailure(
    val userId: Long,
    val taskId: Long,
    val actionId: Long,
    val type: String,
    val localReference: String?,
    val error: String,
    val timestamp: Long,
    val retryCount: Int
)

/**
 * 本地写入恢复的无副作用状态规则。
 *
 * 网络、数据库和偏好存储由调用方处理；该模块只决定哪些失败状态应保留及其展示形式。
 */
object LocalWriteRecoveryState {
    fun retainPending(
        failures: List<LocalWriteRecoveryFailure>,
        pendingActionIds: Set<Long>
    ): List<LocalWriteRecoveryFailure> = failures.filter { it.actionId in pendingActionIds }

    fun nextFailure(
        userId: Long,
        taskId: Long,
        actionId: Long,
        type: String,
        localReference: String?,
        error: String,
        timestamp: Long,
        previousRetryCount: Int
    ): LocalWriteRecoveryFailure = LocalWriteRecoveryFailure(
        userId = userId,
        taskId = taskId,
        actionId = actionId,
        type = type,
        localReference = localReference,
        error = error,
        timestamp = timestamp,
        retryCount = previousRetryCount + 1
    )

    fun toUiItems(failures: List<LocalWriteRecoveryFailure>): List<AgentMessageItem.LocalWriteRecoveryItem> =
        failures.sortedBy { it.timestamp }.map { failure ->
            AgentMessageItem.LocalWriteRecoveryItem(
                taskId = failure.taskId,
                actionId = failure.actionId,
                type = failure.type,
                error = failure.error,
                retryCount = failure.retryCount,
                timestamp = failure.timestamp
            )
        }
}
