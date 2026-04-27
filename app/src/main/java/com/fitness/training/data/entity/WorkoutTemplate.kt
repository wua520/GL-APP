package com.fitness.training.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_templates")
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0, // 用户ID
    val name: String,
    val description: String = "",
    val isPreset: Boolean = false // 是否为预设计划
)



