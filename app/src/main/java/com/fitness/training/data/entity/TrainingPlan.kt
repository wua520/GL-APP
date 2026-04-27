package com.fitness.training.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_plans")
data class TrainingPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0, // 用户ID
    val title: String,
    val description: String,
    val details: String,
    val goal: String, // 增肌/减脂/维持
    val experience: String, // 新手/中级/高级
    val targetMuscles: String = "", // 目标肌群，用逗号分隔
    val trainingDays: Int = 3, // 每周训练天数
    val trainingDuration: String = "标准", // 短时/标准/超长
    val equipment: String = "健身房", // 家庭/健身房
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(), // 编辑时间
    val isPinned: Boolean = false, // 是否置顶
    val isFromRecommendation: Boolean = true // 是否来自推荐计划
)
