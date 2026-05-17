package com.fitness.training.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val muscleGroup: String, // 胸部、背部、腿部、肩部、手臂、核心
    val subMuscleGroup: String = "", // 子分类：上胸、中下胸、股四头等
    val equipment: String, // 杠铃、哑铃、器械、自重等
    val description: String = "",
    val imageUrl: String = "", // 动作图片URL
    val isCustom: Boolean = false, // 是否为自定义动作
    val isFavorite: Boolean = false // 是否收藏
)



