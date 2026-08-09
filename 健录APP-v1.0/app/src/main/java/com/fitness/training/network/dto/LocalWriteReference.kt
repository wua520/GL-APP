package com.fitness.training.network.dto

import com.google.gson.Gson

data class LocalWriteReference(
    val type: String,
    val ids: List<Long>
) {
    companion object {
        private val gson = Gson()

        fun trainingPlan(planId: Long): String = gson.toJson(
            LocalWriteReference(type = "training_plan", ids = listOf(planId))
        )

        fun dietRecords(recordIds: List<Long>): String = gson.toJson(
            LocalWriteReference(type = "diet_records", ids = recordIds)
        )
    }
}
