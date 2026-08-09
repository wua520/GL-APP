package com.fitness.training.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalWriteRecoveryStateTest {

    @Test
    fun retainPendingRemovesResolvedActionsWithoutTouchingPendingFailures() {
        val failures = listOf(
            failure(actionId = 10L, timestamp = 200L),
            failure(actionId = 20L, timestamp = 100L)
        )

        val retained = LocalWriteRecoveryState.retainPending(failures, setOf(20L))

        assertEquals(listOf(20L), retained.map { it.actionId })
    }

    @Test
    fun nextFailureIncrementsRetryCountAndKeepsRecoveryContext() {
        val next = LocalWriteRecoveryState.nextFailure(
            userId = 1L,
            taskId = 2L,
            actionId = 3L,
            type = "CREATE_DIET_RECORD",
            localReference = "{\"type\":\"diet_records\",\"ids\":[4]}",
            error = "网络超时",
            timestamp = 99L,
            previousRetryCount = 2
        )

        assertEquals(3, next.retryCount)
        assertEquals(3L, next.actionId)
        assertEquals("网络超时", next.error)
        assertEquals(99L, next.timestamp)
    }

    @Test
    fun uiProjectionOrdersFailuresByTimestampAndPreservesRetryMetadata() {
        val items = LocalWriteRecoveryState.toUiItems(
            listOf(
                failure(actionId = 10L, timestamp = 200L, retryCount = 2),
                failure(actionId = 20L, timestamp = 100L, retryCount = 4)
            )
        )

        assertEquals(listOf(20L, 10L), items.map { it.actionId })
        assertEquals(4, items.first().retryCount)
        assertEquals("CREATE_TRAINING_PLAN", items.first().type)
    }

    private fun failure(
        actionId: Long,
        timestamp: Long,
        retryCount: Int = 1
    ) = LocalWriteRecoveryFailure(
        userId = 1L,
        taskId = 2L,
        actionId = actionId,
        type = "CREATE_TRAINING_PLAN",
        localReference = null,
        error = "同步失败",
        timestamp = timestamp,
        retryCount = retryCount
    )
}
